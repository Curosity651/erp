package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.converter.TransferOrderConverter;
import com.erp.admin.wms.mapper.TransferOrderItemMapper;
import com.erp.admin.wms.mapper.TransferOrderMapper;
import com.erp.admin.wms.model.dto.TransferOrderDTO;
import com.erp.admin.wms.model.dto.TransferOrderItemDTO;
import com.erp.admin.wms.model.entity.Inventory;
import com.erp.admin.wms.model.entity.TransferOrder;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.enums.TransferOrderStatus;
import com.erp.admin.wms.model.enums.WarehouseTypeEnum;
import com.erp.admin.wms.model.qo.TransferOrderQO;
import com.erp.admin.wms.model.vo.AvailableStockVO;
import com.erp.admin.wms.model.vo.TransferOrderDetailVO;
import com.erp.admin.wms.model.vo.TransferOrderItemVO;
import com.erp.admin.wms.model.vo.TransferOrderPageVO;
import com.erp.admin.wms.model.vo.TransferOrderStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 调拨单服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferOrderService extends ExtendServiceImpl<TransferOrderMapper, TransferOrder> {

	private final TransferOrderItemService transferOrderItemService;

	private final TransferOrderItemMapper transferOrderItemMapper;

	private final WarehouseService warehouseService;

	private final InventoryService inventoryService;

	private final WmsPhysicalInventoryService physicalInventoryService;

	private final WmsLocationService wmsLocationService;

	private final WmsRackAssignmentService wmsRackAssignmentService;

	private final WmsZoneService wmsZoneService;

	private final com.erp.admin.tenant.mapper.SysTenantMapper sysTenantMapper;

	private final StringRedisTemplate stringRedisTemplate;

	/**
	 * Redis 单号序列键前缀
	 */
	private static final String TRANSFER_ORDER_SEQ_KEY_PREFIX = "transfer_order:seq:";

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询条件
	 * @return 分页结果
	 */
	public PageResult<TransferOrderPageVO> queryPage(PageParam pageParam, TransferOrderQO qo) {
		// 使用 Mapper default 方法查询 Entity
		IPage<TransferOrder> page = baseMapper.queryPage(PageUtil.prodPage(pageParam), qo);

		List<TransferOrder> entities = page.getRecords();
		if (entities.isEmpty()) {
			return new PageResult<>(java.util.Collections.emptyList(), page.getTotal());
		}

		// Entity 转 VO
		List<TransferOrderPageVO> records = TransferOrderConverter.INSTANCE.entityListToPageVoList(entities);

		// 填充仓库名称
		enrichWarehouseNames(records);

		// 批量查询明细数据
		List<Long> ids = entities.stream().map(TransferOrder::getId).collect(Collectors.toList());
		List<TransferOrderStatsVO> statsList = transferOrderItemMapper.selectItemSummaryByIds(ids);

		// Service 层聚合：按调拨单ID分组，计算 skuCount 和 totalQuantity
		Map<Long, Long> skuCountMap = statsList.stream()
				.collect(Collectors.groupingBy(
						TransferOrderStatsVO::getTransferOrderId,
						Collectors.counting()
				));

		Map<Long, Integer> totalQuantityMap = statsList.stream()
				.collect(Collectors.groupingBy(
						TransferOrderStatsVO::getTransferOrderId,
						Collectors.summingInt(TransferOrderStatsVO::getQuantity)
				));

		// 填充 skuCount 和 totalQuantity
		for (TransferOrderPageVO vo : records) {
			vo.setSkuCount(skuCountMap.getOrDefault(vo.getId(), 0L).intValue());
			vo.setTotalQuantity(totalQuantityMap.getOrDefault(vo.getId(), 0));
		}

		return new PageResult<>(records, page.getTotal());
	}

	/**
	 * 获取调拨单详情
	 * @param id 调拨单ID
	 * @return 调拨单详情
	 */
	public TransferOrderDetailVO getDetail(Long id) {
		TransferOrder entity = this.getById(id);
		Assert.notNull(entity, "调拨单不存在");

		// Entity 转 VO
		TransferOrderDetailVO detail = TransferOrderConverter.INSTANCE.entityToDetailVo(entity);

		// 填充仓库名称
		enrichWarehouseNamesForDetail(detail);

		// 查询明细列表
		List<TransferOrderItemVO> items = transferOrderItemService.getVoListByTransferOrderId(id);
		detail.setItems(items);

		return detail;
	}

	/**
	 * 生成调拨单号
	 * 格式：TR + 年月日(yyyyMMdd) + 4位序号
	 * 使用 Redis 自增避免并发问题
	 * @return 调拨单号
	 */
	public String generateTransferNo() {
		String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String key = TRANSFER_ORDER_SEQ_KEY_PREFIX + dateStr;

		// Redis 自增，首次设置过期时间为2天
		Long seq = stringRedisTemplate.opsForValue().increment(key);
		if (seq != null && seq == 1) {
			stringRedisTemplate.expire(key, 2, TimeUnit.DAYS);
		}

		return "TR" + dateStr + String.format("%04d", seq != null ? seq : 1);
	}

	/**
	 * 创建调拨单
	 * @param dto 调拨单DTO
	 * @return 调拨单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long create(TransferOrderDTO dto) {
		// 校验明细不为空
		Assert.notEmpty(dto.getItems(), "调拨明细不能为空");

		// 校验仓库
		validateWarehouses(dto);

		// 校验批次/库位级明细（源批次 + 目标库位服务商租架），并从批次回填 skuCode
		validateAndEnrichItems(dto.getFromWarehouseId(), dto.getToWarehouseId(), dto.getErpTenantId(), dto.getItems());

		// 转换实体
		TransferOrder order = TransferOrderConverter.INSTANCE.dtoToEntity(dto);

		// 生成调拨单号
		order.setTransferNo(generateTransferNo());

		// 设置初始状态
		order.setOrderStatus(TransferOrderStatus.DRAFT.getCode());

		// 保存调拨单
		this.save(order);

		// 保存明细
		transferOrderItemService.batchSave(order.getId(), dto.getErpTenantId(), dto.getItems());

		log.info("Created transfer order, id={}, transferNo={}", order.getId(), order.getTransferNo());

		return order.getId();
	}

	/**
	 * 编辑调拨单
	 * @param dto 调拨单DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void update(TransferOrderDTO dto) {
		TransferOrder order = this.getById(dto.getId());
		Assert.notNull(order, "调拨单不存在");

		// 只有草稿状态可以编辑
		Assert.isTrue(TransferOrderStatus.DRAFT.getCode().equals(order.getOrderStatus()),
				"只有草稿状态的调拨单可以编辑");

		// 校验明细不为空
		Assert.notEmpty(dto.getItems(), "调拨明细不能为空");

		// 校验仓库
		validateWarehouses(dto);

		// 校验批次/库位级明细（源批次 + 目标库位服务商租架），并从批次回填 skuCode
		validateAndEnrichItems(dto.getFromWarehouseId(), dto.getToWarehouseId(), dto.getErpTenantId(), dto.getItems());

		// 删除原有明细
		transferOrderItemService.deleteByTransferOrderId(order.getId());

		// 更新调拨单
		TransferOrderConverter.INSTANCE.updateEntity(dto, order);
		this.updateById(order);

		// 保存新明细
		transferOrderItemService.batchSave(order.getId(), dto.getErpTenantId(), dto.getItems());

		log.info("Updated transfer order, id={}, transferNo={}", order.getId(), order.getTransferNo());
	}

	/**
	 * 删除调拨单
	 * @param id 调拨单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void delete(Long id) {
		TransferOrder order = this.getById(id);
		Assert.notNull(order, "调拨单不存在");

		// 使用枚举 isDeletable() 判断
		TransferOrderStatus status = TransferOrderStatus.fromCode(order.getOrderStatus());
		Assert.isTrue(status.isDeletable(), "只有草稿或已取消状态的调拨单可以删除");

		// 删除明细
		transferOrderItemService.deleteByTransferOrderId(id);

		// 删除调拨单
		this.removeById(id);

		log.info("Transfer order deleted, id={}, transferNo={}", order.getId(), order.getTransferNo());
	}

	/**
	 * 分页查询可调拨库存
	 * @param pageParam 分页参数
	 * @param warehouseId 仓库ID
	 * @param keyword 关键字（SKU编码或名称）
	 * @return 分页结果
	 */
	public PageResult<AvailableStockVO> pageAvailableStock(PageParam pageParam, Long warehouseId, String keyword) {
		return inventoryService.pageAvailableStock(pageParam, warehouseId, keyword);
	}

	/**
	 * 批量查询可用库存
	 * @param warehouseId 仓库ID
	 * @param skuCodes SKU编码列表
	 * @return skuCode -> availableQuantity 映射
	 */
	public Map<String, Integer> batchQueryStock(Long warehouseId, List<String> skuCodes) {
		return inventoryService.getAvailableStockMap(warehouseId, skuCodes);
	}

	/**
	 * 校验仓库
	 * @param dto 调拨单DTO
	 */
	private void validateWarehouses(TransferOrderDTO dto) {
		// 校验源仓库
		Warehouse fromWarehouse = warehouseService.getById(dto.getFromWarehouseId());
		Assert.notNull(fromWarehouse, "源仓库不存在");
		Assert.isTrue(WarehouseTypeEnum.OWN.getCode().equals(fromWarehouse.getWarehouseType()), "源仓库必须是自有仓");

		// 校验目标仓库
		Warehouse toWarehouse = warehouseService.getById(dto.getToWarehouseId());
		Assert.notNull(toWarehouse, "目标仓库不存在");

		// 校验源仓库和目标仓库不能相同
		Assert.isTrue(!dto.getFromWarehouseId().equals(dto.getToWarehouseId()), "源仓库和目标仓库不能相同");

		// 系统仅保留普通调拨：目标仓库必须是自有仓
		Assert.isTrue(WarehouseTypeEnum.OWN.getCode().equals(toWarehouse.getWarehouseType()),
				"调拨的目标仓库必须是自有仓");
	}

	/**
	 * 校验批次/库位级明细（建单即拦）：逐条校验源批次(源仓、货主、非虚拟收纳、可用足量)并从批次带出 skuCode；
	 * 校验目标库位落在【该货主服务商在目标仓当前有效租用排】上，无租架则不能调拨。
	 * @param fromWarehouseId 源仓库ID
	 * @param toWarehouseId   目标仓库ID
	 * @param erpTenantId     货主
	 * @param items           明细DTO列表（原地回填 skuCode）
	 */
	private void validateAndEnrichItems(Long fromWarehouseId, Long toWarehouseId, Long erpTenantId,
			List<TransferOrderItemDTO> items) {
		Long operatorId = ownerOperatorId(erpTenantId);
		Assert.notNull(operatorId, "该货主未绑定服务商，无法调拨");
		java.util.Set<String> targetRacks = wmsRackAssignmentService.activeRackNos(toWarehouseId, operatorId);
		Assert.isTrue(!targetRacks.isEmpty(), "该货主的服务商在目标仓无租用库位，不能调拨到该仓");
		Map<String, com.erp.admin.wms.model.entity.WmsLocation> toLocByCode = wmsLocationService
				.listByWarehouse(toWarehouseId).stream()
				.filter(l -> l.getLocationCode() != null)
				.collect(Collectors.toMap(com.erp.admin.wms.model.entity.WmsLocation::getLocationCode, l -> l,
						(a, b) -> a));

		for (TransferOrderItemDTO item : items) {
			Assert.notNull(item.getSourcePhysicalInventoryId(), "源批次不能为空");
			com.erp.admin.wms.model.entity.WmsPhysicalInventory batch = physicalInventoryService
					.getById(item.getSourcePhysicalInventoryId());
			Assert.notNull(batch, "源批次不存在：" + item.getSourcePhysicalInventoryId());
			Assert.isTrue(fromWarehouseId.equals(batch.getWarehouseId()), "源批次不属于源仓库");
			Assert.isTrue(erpTenantId.equals(batch.getErpTenantId()), "源批次不属于该货主");
			Assert.isTrue(batch.getContainerStored() == null || batch.getContainerStored() != 1,
					"该批次已在虚拟库位收纳，请先取回再调拨");
			int reserved = batch.getReservedQty() == null ? 0 : batch.getReservedQty();
			int available = batch.getQuantity() - reserved;
			Assert.isTrue(item.getQuantity() != null && item.getQuantity() > 0 && item.getQuantity() <= available,
					String.format("批次[%s]调拨数量(%s)超过可用(%d)", batch.getLocationCode(), item.getQuantity(), available));
			// 从批次带出 SKU（明细 skuCode 权威取批次）
			item.setSkuCode(batch.getSkuCode());

			// 目标库位校验
			Assert.hasText(item.getTargetLocationCode(), "目标库位不能为空");
			com.erp.admin.wms.model.entity.WmsLocation target = toLocByCode.get(item.getTargetLocationCode());
			Assert.notNull(target, "目标仓库无此库位：" + item.getTargetLocationCode());
			Assert.isTrue(targetRacks.contains(target.getRackNo()),
					"目标库位不在该货主服务商于目标仓的租用范围：" + item.getTargetLocationCode());
		}
	}

	/** 货主 → 父服务商 wms_tenant_id。 */
	private Long ownerOperatorId(Long erpTenantId) {
		if (erpTenantId == null) {
			return null;
		}
		com.erp.admin.tenant.model.entity.SysTenant owner = sysTenantMapper.selectById(erpTenantId);
		return owner == null ? null : owner.getParentWmsTenantId();
	}

	/**
	 * 源批次候选（A 仓，某货主可调拨的批次：良品 + 未收纳虚拟库位 + 有余量）。供表单选源批次。
	 */
	public List<com.erp.admin.wms.model.entity.WmsPhysicalInventory> listSourceBatches(Long fromWarehouseId,
			Long erpTenantId, String skuKeyword) {
		if (fromWarehouseId == null || erpTenantId == null) {
			return java.util.Collections.emptyList();
		}
		return physicalInventoryService.listByErpTenant(erpTenantId, fromWarehouseId, skuKeyword).stream()
				.filter(b -> b.getQuantity() != null
						&& b.getQuantity() - (b.getReservedQty() == null ? 0 : b.getReservedQty()) > 0)
				.filter(b -> b.getContainerStored() == null || b.getContainerStored() != 1)
				.filter(b -> "GOOD".equals(b.getQuality()))
				.collect(Collectors.toList());
	}

	/**
	 * 目标库位候选（B 仓，落在该货主服务商当前有效租用排上的标准区库位）。服务商在 B 无租架则返回空 → 不能调拨。
	 */
	public List<com.erp.admin.wms.model.vo.AvailableLocationVO> listTargetLocations(Long toWarehouseId,
			Long erpTenantId) {
		Long operatorId = ownerOperatorId(erpTenantId);
		if (toWarehouseId == null || operatorId == null) {
			return java.util.Collections.emptyList();
		}
		java.util.Set<String> racks = wmsRackAssignmentService.activeRackNos(toWarehouseId, operatorId);
		if (racks.isEmpty()) {
			return java.util.Collections.emptyList();
		}
		Map<Long, com.erp.admin.wms.model.entity.WmsZone> zoneById = wmsZoneService.listByWarehouse(toWarehouseId)
				.stream()
				.collect(Collectors.toMap(com.erp.admin.wms.model.entity.WmsZone::getId, z -> z, (a, b) -> a));
		List<com.erp.admin.wms.model.vo.AvailableLocationVO> result = new java.util.ArrayList<>();
		for (com.erp.admin.wms.model.entity.WmsLocation l : wmsLocationService.listByWarehouse(toWarehouseId)) {
			if (l.getIsVirtual() != null && l.getIsVirtual() == 1) {
				continue;
			}
			if (!racks.contains(l.getRackNo())) {
				continue;
			}
			com.erp.admin.wms.model.entity.WmsZone z = l.getZoneId() == null ? null : zoneById.get(l.getZoneId());
			if (z == null || !"STANDARD".equals(z.getZoneType())) {
				continue;
			}
			com.erp.admin.wms.model.vo.AvailableLocationVO vo = new com.erp.admin.wms.model.vo.AvailableLocationVO();
			vo.setLocationId(l.getId());
			vo.setLocationCode(l.getLocationCode());
			vo.setZoneId(l.getZoneId());
			vo.setZoneName(z.getZoneName());
			vo.setZoneType(z.getZoneType());
			vo.setRackNo(l.getRackNo());
			vo.setColumnNo(l.getColumnNo());
			vo.setIsVirtual(0);
			result.add(vo);
		}
		return result;
	}

	/**
	 * 填充分页 VO 的仓库名称
	 */
	private void enrichWarehouseNames(List<TransferOrderPageVO> records) {
		warehouseService.enrichWarehouseDisplay(
				records,
				TransferOrderPageVO::getFromWarehouseId,
				(vo, display) -> vo.setFromWarehouseName(display != null ? display.getWarehouseName() : null)
		);
		warehouseService.enrichWarehouseDisplay(
				records,
				TransferOrderPageVO::getToWarehouseId,
				(vo, display) -> vo.setToWarehouseName(display != null ? display.getWarehouseName() : null)
		);
	}

	/**
	 * 填充详情 VO 的仓库名称
	 */
	private void enrichWarehouseNamesForDetail(TransferOrderDetailVO detail) {
		Warehouse fromWarehouse = warehouseService.getById(detail.getFromWarehouseId());
		if (fromWarehouse != null) {
			detail.setFromWarehouseName(fromWarehouse.getWarehouseName());
		}
		Warehouse toWarehouse = warehouseService.getById(detail.getToWarehouseId());
		if (toWarehouse != null) {
			detail.setToWarehouseName(toWarehouse.getWarehouseName());
		}
	}

}
