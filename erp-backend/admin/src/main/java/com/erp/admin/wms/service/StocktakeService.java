package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.product.model.vo.SkuBriefVO;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import org.ballcat.common.core.exception.BusinessException;
import com.erp.admin.wms.converter.StocktakeConverter;
import com.erp.admin.wms.mapper.StocktakeMapper;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.model.dto.StocktakeExtraItemDTO;
import com.erp.admin.wms.model.dto.StocktakeAddSkuDTO;
import com.erp.admin.wms.model.dto.StocktakeDTO;
import com.erp.admin.wms.model.dto.StocktakeItemInputDTO;
import com.erp.admin.wms.model.dto.StocktakeItemsDTO;
import com.erp.admin.wms.model.dto.StockPostingDTO;
import com.erp.admin.wms.model.dto.StockPostingItemDTO;
import com.erp.admin.wms.model.entity.Inventory;
import com.erp.admin.wms.model.entity.StocktakeOrder;
import com.erp.admin.wms.model.entity.StocktakeOrderItem;
import com.erp.admin.wms.model.entity.StocktakeLocationTask;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.enums.SourceType;
import com.erp.admin.wms.model.enums.PostingType;
import com.erp.admin.wms.model.enums.StockBucket;
import com.erp.admin.wms.model.enums.StockDirection;
import com.erp.admin.wms.model.enums.StocktakeItemSource;
import com.erp.admin.wms.model.enums.StocktakeItemStatus;
import com.erp.admin.wms.model.enums.StocktakeScope;
import com.erp.admin.wms.model.enums.StocktakeStatus;
import com.erp.admin.wms.model.enums.StocktakeMode;
import com.erp.admin.wms.model.enums.StocktakeTaskStatus;
import com.erp.admin.wms.model.qo.StocktakeQO;
import com.erp.admin.wms.model.vo.*;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.service.SkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.util.Assert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * 盘点单服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StocktakeService extends ExtendServiceImpl<StocktakeMapper, StocktakeOrder> {

	/**
	 * 单号生成最大重试次数
	 */
	private static final int MAX_RETRY_ATTEMPTS = 3;

	private final StocktakeItemService stocktakeItemService;

	private final StocktakeLocationTaskService stocktakeLocationTaskService;

	private final WmsLocationService wmsLocationService;

	private final WmsPhysicalInventoryMapper physicalInventoryMapper;

	private final WmsInventoryAggregator inventoryAggregator;

	private final WmsZoneService wmsZoneService;

	private final WmsRackAssignmentService wmsRackAssignmentService;

	private final ObjectMapper objectMapper;

	private final SysTenantMapper sysTenantMapper;

	private final StockPostingService stockPostingService;

	private final InventoryService inventoryService;

	private final WarehouseService warehouseService;

	private final SkuService skuService;

	private final SkuBriefService skuBriefService;

	private final PrincipalAttributeAccessor principalAttributeAccessor;

	private final TenantIdentityService tenantIdentityService;

	/**
	 * 分页查询
	 *
	 * @param pageParam 分页参数
	 * @param qo        查询条件
	 * @return 分页结果
	 */
	public PageResult<StocktakePageVO> queryPage(PageParam pageParam, StocktakeQO qo) {
		assertPlatform();
		IPage<StocktakePageVO> page = PageUtil.prodPage(pageParam);
		baseMapper.queryPage(page, qo);

		List<StocktakePageVO> records = page.getRecords();
		if (records.isEmpty()) {
			return new PageResult<>(records, page.getTotal());
		}

		// 批量查询统计信息
		List<Long> ids = records.stream().map(StocktakePageVO::getId).collect(Collectors.toList());
		Map<Long, StocktakeStatsVO> statsMap = baseMapper.selectStatsByIds(ids)
				.stream()
				.collect(Collectors.toMap(StocktakeStatsVO::getStocktakeOrderId, Function.identity()));

		// 填充统计信息
		for (StocktakePageVO vo : records) {
			StocktakeStatsVO stats = statsMap.get(vo.getId());
			if (stats != null) {
				vo.setSkuCount(stats.getSkuCount());
				vo.setDiffCount(stats.getDiffCount());
				vo.setCountedCount(stats.getCountedCount());
			} else {
				vo.setSkuCount(0);
				vo.setDiffCount(0);
				vo.setCountedCount(0);
			}
			List<StocktakeLocationTask> tasks = stocktakeLocationTaskService.listByStocktakeId(vo.getId());
			vo.setLocationCount(tasks.size());
			vo.setCompletedLocationCount((int) tasks.stream().filter(task ->
					StocktakeTaskStatus.COMPLETED.name().equals(task.getTaskStatus())
							|| StocktakeTaskStatus.REVIEWED.name().equals(task.getTaskStatus())).count());
		}

		return new PageResult<>(records, page.getTotal());
	}

	/**
	 * 获取盘点单详情
	 *
	 * @param id 盘点单ID
	 * @return 盘点单详情
	 */
	public StocktakeDetailVO getDetail(Long id) {
		assertPlatform();
		StocktakeDetailVO detail = baseMapper.selectDetailById(id);
		Assert.notNull(detail, "盘点单不存在");

		// 查询明细列表
		List<StocktakeItemVO> items = stocktakeItemService.getVoListByStocktakeOrderId(id);
		detail.setItems(items);

		// 填充 SKU 总数和差异总数
		detail.setSkuCount(items.size());

		int diffCount = 0;
		int countedCount = 0;
		for (StocktakeItemVO item : items) {
			if (StocktakeItemStatus.COUNTED.name().equals(item.getStocktakeStatus())) {
				countedCount++;
			}
			if (item.getDiffQuantity() != null && item.getDiffQuantity() != 0) {
				diffCount++;
			}
		}
		detail.setDiffCount(diffCount);
		detail.setCountedCount(countedCount);
		List<StocktakeLocationTask> tasks = stocktakeLocationTaskService.listByStocktakeId(id);
		detail.setLocationCount(tasks.size());
		detail.setCompletedLocationCount((int) tasks.stream().filter(task ->
				StocktakeTaskStatus.COMPLETED.name().equals(task.getTaskStatus())
						|| StocktakeTaskStatus.REVIEWED.name().equals(task.getTaskStatus())).count());

		return detail;
	}

	/**
	 * 生成盘点单号
	 * 格式：ST + 年月日(yyyyMMdd) + 4位序号
	 *
	 * @return 盘点单号
	 */
	public String generateStocktakeNo() {
		String prefix = "ST" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		int count = baseMapper.countTodayOrders(prefix);
		return prefix + String.format("%04d", count + 1);
	}

	/**
	 * 检查仓库是否有进行中的盘点单
	 *
	 * @param warehouseId 仓库ID
	 */
	private void checkWarehouseStocktakeInProgress(Long warehouseId) {
		long count = baseMapper.countInProgressByWarehouse(warehouseId);
		Assert.isTrue(count == 0, "该仓库已有进行中的盘点单，请先完成或取消后再创建新的盘点单");
	}

	/**
	 * 创建库位盘点单并生成任务。
	 *
	 * @param dto 盘点单DTO
	 * @return 盘点单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long create(StocktakeDTO dto) {
		assertPlatform();
		StocktakeMode mode = StocktakeMode.valueOf(dto.getStocktakeMode());
		// 校验仓库
		Assert.notNull(warehouseService.getById(dto.getWarehouseId()), "仓库不存在");

		// 检查仓库是否有进行中的盘点单
		checkWarehouseStocktakeInProgress(dto.getWarehouseId());

		// 转换实体
		StocktakeOrder order = StocktakeConverter.INSTANCE.dtoToEntity(dto);
		order.setStocktakeMode(mode.name());
		order.setStocktakeScope(mode == StocktakeMode.FULL ? StocktakeScope.ALL.name() : StocktakeScope.PARTIAL.name());
		order.setBlindCount(Boolean.FALSE.equals(dto.getBlindCount()) ? 0 : 1);
		order.setFreezeMode(mode == StocktakeMode.FULL ? "WAREHOUSE" : "LOCATION");
		order.setSnapshotTime(LocalDateTime.now());
		order.setScopeConfig(toScopeConfig(dto));

		// 生成盘点单号
		order.setStocktakeNo(generateStocktakeNo());

		// 设置初始状态为盘点中
		order.setOrderStatus(StocktakeStatus.COUNTING.name());

		// 保存盘点单（带重试机制处理单号冲突）
		saveOrderWithRetry(order);

		log.info("Created stocktake order, id={}, stocktakeNo={}", order.getId(), order.getStocktakeNo());

		generateLocationTasks(order, dto, mode);

		return order.getId();
	}

	private String toScopeConfig(StocktakeDTO dto) {
		Map<String, Object> scope = new LinkedHashMap<>();
		scope.put("locationIds", dto.getLocationIds());
		scope.put("specialSkuCodes", dto.getSpecialSkuCodes());
		scope.put("specialOwnerId", dto.getSpecialOwnerId());
		scope.put("specialSearchAll", dto.getSpecialSearchAll());
		try {
			return objectMapper.writeValueAsString(scope);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("盘点范围配置无效", ex);
		}
	}

	private void generateLocationTasks(StocktakeOrder order, StocktakeDTO dto, StocktakeMode mode) {
		List<WmsLocation> allLocations = wmsLocationService.listByWarehouse(order.getWarehouseId()).stream()
				.filter(location -> !Integer.valueOf(1).equals(location.getIsVirtual()))
				.collect(Collectors.toList());
		Assert.notEmpty(allLocations, "仓库没有可盘点的物理库位");

		List<WmsPhysicalInventory> allBatches = physicalInventoryMapper.listByWarehouse(order.getWarehouseId());
		Set<Long> selectedLocationIds = selectLocationIds(dto, mode, allLocations, allBatches);
		List<WmsLocation> selectedLocations = allLocations.stream()
				.filter(location -> selectedLocationIds.contains(location.getId()))
				.collect(Collectors.toList());
		Assert.notEmpty(selectedLocations, "盘点范围没有匹配的库位");

		List<StocktakeLocationTask> tasks = new ArrayList<>();
		for (WmsLocation location : selectedLocations) {
			StocktakeLocationTask task = new StocktakeLocationTask();
			task.setStocktakeOrderId(order.getId());
			task.setWarehouseId(order.getWarehouseId());
			task.setZoneId(location.getZoneId());
			task.setLocationId(location.getId());
			task.setLocationCode(location.getLocationCode());
			task.setTaskStatus(StocktakeTaskStatus.PENDING.name());
			task.setLocationConfirmed(0);
			task.setVersion(0);
			tasks.add(task);
		}
		stocktakeLocationTaskService.saveBatch(tasks);

		Map<String, StocktakeLocationTask> taskByLocation = tasks.stream().collect(Collectors.toMap(
				StocktakeLocationTask::getLocationCode, Function.identity(), (a, b) -> a, LinkedHashMap::new));
		List<StocktakeOrderItem> items = allBatches.stream()
				.filter(batch -> taskByLocation.containsKey(batch.getLocationCode()))
				.filter(batch -> includeBatchForMode(batch, dto, mode))
				.filter(batch -> nz(batch.getQuantity()) > 0 || nz(batch.getReservedQty()) > 0)
				.map(batch -> toStocktakeItem(order.getId(), taskByLocation.get(batch.getLocationCode()), batch))
				.collect(Collectors.toList());
		if (!items.isEmpty()) {
			stocktakeItemService.saveBatch(items);
		}
		log.info("Generated location stocktake, stocktakeId={}, mode={}, taskCount={}, itemCount={}",
				order.getId(), mode, tasks.size(), items.size());
	}

	private Set<Long> selectLocationIds(StocktakeDTO dto, StocktakeMode mode, List<WmsLocation> locations,
			List<WmsPhysicalInventory> batches) {
		if (mode == StocktakeMode.FULL || (mode == StocktakeMode.SPECIAL && Boolean.TRUE.equals(dto.getSpecialSearchAll()))) {
			return locations.stream().map(WmsLocation::getId).collect(Collectors.toCollection(LinkedHashSet::new));
		}
		if (mode == StocktakeMode.CYCLE) {
			Assert.notEmpty(dto.getLocationIds(), "循环盘点必须选择库位");
			Set<Long> validIds = locations.stream().map(WmsLocation::getId).collect(Collectors.toSet());
			Assert.isTrue(validIds.containsAll(dto.getLocationIds()), "选择的库位不属于当前仓库");
			return new LinkedHashSet<>(dto.getLocationIds());
		}
		Set<String> locationCodes = batches.stream()
				.filter(batch -> includeBatchForMode(batch, dto, mode))
				.map(WmsPhysicalInventory::getLocationCode)
				.filter(code -> code != null && !code.isEmpty())
				.collect(Collectors.toSet());
		return locations.stream().filter(location -> locationCodes.contains(location.getLocationCode()))
				.map(WmsLocation::getId).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private boolean includeBatchForMode(WmsPhysicalInventory batch, StocktakeDTO dto, StocktakeMode mode) {
		if (mode != StocktakeMode.SPECIAL) {
			return true;
		}
		boolean skuMatches = dto.getSpecialSkuCodes() == null || dto.getSpecialSkuCodes().isEmpty()
				|| dto.getSpecialSkuCodes().contains(batch.getSkuCode());
		boolean ownerMatches = dto.getSpecialOwnerId() == null || dto.getSpecialOwnerId().equals(batch.getErpTenantId());
		return skuMatches && ownerMatches;
	}

	private StocktakeOrderItem toStocktakeItem(Long stocktakeId, StocktakeLocationTask task,
			WmsPhysicalInventory batch) {
		StocktakeOrderItem item = new StocktakeOrderItem();
		item.setStocktakeOrderId(stocktakeId);
		item.setLocationTaskId(task.getId());
		item.setPhysicalInventoryId(batch.getId());
		item.setErpTenantId(batch.getErpTenantId());
		item.setWmsTenantId(batch.getWmsTenantId() == null ? 0L : batch.getWmsTenantId());
		item.setSkuCode(batch.getSkuCode());
		item.setZoneId(batch.getZoneId());
		item.setLocationCode(batch.getLocationCode());
		item.setQuality(batch.getQuality());
		item.setAllocatable(batch.getAllocatable());
		item.setInboundDate(batch.getInboundDate());
		item.setPickOrder(batch.getPickOrder());
		item.setSystemQuantity(nz(batch.getQuantity()));
		item.setReservedQuantity(nz(batch.getReservedQty()));
		item.setSourceType(StocktakeItemSource.EXISTING.name());
		item.setStocktakeStatus(StocktakeItemStatus.PENDING.name());
		return item;
	}

	private int nz(Integer value) {
		return value == null ? 0 : value;
	}

	/**
	 * 生成盘点明细（仅用于全部SKU盘点）
	 *
	 * @param stocktakeId 盘点单ID
	 * @param warehouseId 仓库ID
	 */
	private void generateStocktakeItems(Long stocktakeId, Long warehouseId) {
		// 获取仓库所有有记录的SKU（含库存为0的）
		List<Inventory> inventories = inventoryService.getAllByWarehouseId(warehouseId);
		Assert.notEmpty(inventories, "没有可盘点的库存");

		// 获取所有 SKU 信息
		List<String> skuCodes = inventories.stream()
				.map(Inventory::getSkuCode)
				.collect(Collectors.toList());
		List<Sku> skus = skuService.listBySkuCodes(skuCodes);
		Map<String, Sku> skuMap = skus.stream()
				.collect(Collectors.toMap(Sku::getSkuCode, Function.identity()));

		// 生成盘点明细
		List<StocktakeOrderItem> items = inventories.stream()
				.map(inv -> {
					StocktakeOrderItem item = new StocktakeOrderItem();
					item.setErpTenantId(inv.getErpTenantId());
					item.setSkuCode(inv.getSkuCode());
					// 计算系统库存 = 可用 + 占用（不含在途）
					int systemQty = inv.getAvailableQuantity() + inv.getReservedQuantity();
					item.setSystemQuantity(systemQty);
					item.setSourceType(StocktakeItemSource.EXISTING.name());
					item.setStocktakeStatus(StocktakeItemStatus.PENDING.name());
					return item;
				})
				.collect(Collectors.toList());

		// 保存明细
		stocktakeItemService.batchSave(stocktakeId, items);

		log.info("Generated stocktake items, stocktakeId={}, itemCount={}", stocktakeId, items.size());
	}

	/**
	 * 获取盘点明细
	 *
	 * @param id 盘点单ID
	 * @return 盘点明细列表
	 */
	public List<StocktakeItemVO> getItems(Long id) {
		assertPlatform();
		StocktakeOrder order = this.getById(id);
		Assert.notNull(order, "盘点单不存在");
		return stocktakeItemService.getVoListByStocktakeOrderId(id);
	}

	public List<StocktakeLocationTaskVO> getTasks(Long id) {
		assertPlatform();
		StocktakeOrder order = this.getById(id);
		Assert.notNull(order, "盘点单不存在");
		Map<Long, List<StocktakeOrderItem>> itemsByTask = stocktakeItemService.getByStocktakeOrderId(id).stream()
				.filter(item -> item.getLocationTaskId() != null)
				.collect(Collectors.groupingBy(StocktakeOrderItem::getLocationTaskId));
		return stocktakeLocationTaskService.listByStocktakeId(id).stream().map(task -> {
			StocktakeLocationTaskVO vo = new StocktakeLocationTaskVO();
			vo.setId(task.getId());
			vo.setStocktakeOrderId(task.getStocktakeOrderId());
			vo.setWarehouseId(task.getWarehouseId());
			vo.setZoneId(task.getZoneId());
			vo.setLocationId(task.getLocationId());
			vo.setLocationCode(task.getLocationCode());
			vo.setTaskStatus(task.getTaskStatus());
			vo.setAssigneeName(task.getAssigneeName());
			vo.setCompletedTime(task.getCompletedTime());
			List<StocktakeOrderItem> items = itemsByTask.getOrDefault(task.getId(), Collections.emptyList());
			vo.setItemCount(items.size());
			vo.setCountedCount((int) items.stream().filter(item -> item.getActualQuantity() != null).count());
			vo.setDiffCount((int) items.stream().filter(item -> item.getDiffQuantity() != null
					&& item.getDiffQuantity() != 0).count());
			return vo;
		}).collect(Collectors.toList());
	}

	public List<StocktakeItemVO> getTaskItems(Long taskId) {
		assertPlatform();
		StocktakeLocationTask task = stocktakeLocationTaskService.getById(taskId);
		Assert.notNull(task, "盘点库位任务不存在");
		return stocktakeItemService.getVoListByTaskId(taskId);
	}

	public List<Long> getEligibleOwnerIds(Long taskId) {
		assertPlatform();
		StocktakeLocationTask task = stocktakeLocationTaskService.getById(taskId);
		Assert.notNull(task, "盘点库位任务不存在");
		WmsLocation location = wmsLocationService.getById(task.getLocationId());
		Assert.notNull(location, "盘点库位不存在");
		Long operatorId = wmsRackAssignmentService.activeOperatorId(task.getWarehouseId(), location.getRackNo());
		if (operatorId == null) {
			return Collections.emptyList();
		}
		return sysTenantMapper.listEnabledErpTenantIdsByParent(operatorId);
	}

	@Transactional(rollbackFor = Exception.class)
	public StocktakeItemVO addExtraItem(StocktakeExtraItemDTO dto) {
		assertPlatform();
		StocktakeLocationTask task = stocktakeLocationTaskService.getById(dto.getTaskId());
		Assert.notNull(task, "盘点库位任务不存在");
		StocktakeOrder order = this.getById(task.getStocktakeOrderId());
		Assert.notNull(order, "盘点单不存在");
		Assert.isTrue(StocktakeStatus.COUNTING.name().equals(order.getOrderStatus()), "只有盘点中可以新增账外商品");
		Assert.isTrue(!StocktakeTaskStatus.COMPLETED.name().equals(task.getTaskStatus()), "已完成库位不能新增商品");
		Assert.isTrue(dto.getErpTenantId() > 0, "货主必须为有效正数");
		SysTenant owner = sysTenantMapper.selectById(dto.getErpTenantId());
		Assert.isTrue(owner != null && owner.getParentWmsTenantId() != null,
				"该货主未绑定服务商，无法登记账外库存");
		WmsLocation location = wmsLocationService.getById(task.getLocationId());
		Assert.notNull(location, "盘点库位不存在");
		Assert.isTrue(wmsRackAssignmentService.activeRackNos(task.getWarehouseId(), owner.getParentWmsTenantId())
				.contains(location.getRackNo()),
				"所选货主无权使用当前库位，库位=" + task.getLocationCode());
		Sku sku = TenantContext.runAs(dto.getErpTenantId(), () -> skuService.getBySkuCode(dto.getSkuCode()));
		Assert.notNull(sku, "SKU主数据不存在，请先创建SKU");

		boolean duplicate = stocktakeItemService.getByTaskId(task.getId()).stream()
				.anyMatch(item -> item.getErpTenantId().equals(dto.getErpTenantId())
						&& item.getSkuCode().equals(dto.getSkuCode())
						&& item.getPhysicalInventoryId() == null);
		Assert.isTrue(!duplicate, "该账外商品已添加到当前库位");

		StocktakeOrderItem item = new StocktakeOrderItem();
		item.setStocktakeOrderId(order.getId());
		item.setLocationTaskId(task.getId());
		item.setErpTenantId(dto.getErpTenantId());
		item.setWmsTenantId(owner.getParentWmsTenantId());
		item.setSkuCode(dto.getSkuCode());
		item.setZoneId(task.getZoneId());
		item.setLocationCode(task.getLocationCode());
		item.setQuality(dto.getQuality() == null ? "GOOD" : dto.getQuality());
		WmsZone zone = task.getZoneId() == null ? null : wmsZoneService.getById(task.getZoneId());
		item.setAllocatable("GOOD".equalsIgnoreCase(item.getQuality()) && zone != null
				&& Integer.valueOf(1).equals(zone.getAllocatable()) ? 1 : 0);
		item.setInboundDate(dto.getInboundDate() == null ? order.getStocktakeDate() : dto.getInboundDate());
		item.setSystemQuantity(0);
		item.setReservedQuantity(0);
		item.setActualQuantity(dto.getActualQuantity());
		item.setDiffQuantity(dto.getActualQuantity());
		item.setStocktakeStatus(StocktakeItemStatus.COUNTED.name());
		item.setSourceType(StocktakeItemSource.ADDED.name());
		item.setReviewStatus("PENDING");
		item.setRemark(dto.getRemark());
		stocktakeItemService.save(item);
		return stocktakeItemService.convertToVoList(Collections.singletonList(item)).get(0);
	}

	@Transactional(rollbackFor = Exception.class)
	public void completeTask(Long taskId) {
		assertPlatform();
		StocktakeLocationTask task = stocktakeLocationTaskService.getById(taskId);
		Assert.notNull(task, "盘点库位任务不存在");
		StocktakeOrder order = this.getById(task.getStocktakeOrderId());
		Assert.notNull(order, "盘点单不存在");
		Assert.isTrue(StocktakeStatus.COUNTING.name().equals(order.getOrderStatus()), "只有盘点中可以完成库位");
		List<StocktakeOrderItem> items = stocktakeItemService.getByTaskId(taskId);
		Assert.isTrue(items.stream().allMatch(item -> item.getActualQuantity() != null), "当前库位仍有未盘商品");
		task.setTaskStatus(StocktakeTaskStatus.COMPLETED.name());
		task.setLocationConfirmed(1);
		task.setCompletedTime(LocalDateTime.now());
		Assert.isTrue(stocktakeLocationTaskService.updateById(task), "库位任务并发更新失败，请刷新重试");
	}

	@Transactional(rollbackFor = Exception.class)
	public void submitReview(Long id) {
		assertPlatform();
		StocktakeOrder order = this.getById(id);
		Assert.notNull(order, "盘点单不存在");
		Assert.isTrue(StocktakeStatus.COUNTING.name().equals(order.getOrderStatus()), "只有盘点中可以提交复核");
		Assert.isTrue(stocktakeLocationTaskService.allCompleted(id), "仍有库位任务未完成");
		Assert.isTrue(stocktakeItemService.getByStocktakeOrderId(id).stream()
				.allMatch(item -> item.getActualQuantity() != null), "仍有盘点明细未录入");
		order.setOrderStatus(StocktakeStatus.REVIEWING.name());
		this.updateById(order);
	}

	/**
	 * 获取盘点进度
	 *
	 * @param id 盘点单ID
	 * @return 盘点进度
	 */
	public StocktakeProgressVO getProgress(Long id) {
		assertPlatform();
		StocktakeOrder order = this.getById(id);
		Assert.notNull(order, "盘点单不存在");
		return stocktakeItemService.getProgress(id);
	}

	/**
	 * 越权守卫：盘点为海外仓平台专属作业，仅平台身份可进入
	 * （防菜单权限被误授予货主/服务商后跨货主枚举/调整库存——M-4）。
	 */
	private void assertPlatform() {
		String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
		if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identityType)) {
			throw new BusinessException(403, "仅海外仓平台可执行库存盘点");
		}
	}

	/**
	 * 录入盘点数据
	 *
	 * @param dto 盘点录入DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void saveItems(StocktakeItemsDTO dto) {
		assertPlatform();
		StocktakeOrder order = this.getById(dto.getStocktakeId());
		Assert.notNull(order, "盘点单不存在");

		// 只有盘点中状态可以录入
		Assert.isTrue(StocktakeStatus.COUNTING.name().equals(order.getOrderStatus()),
				"只有盘点中状态的盘点单可以录入数据");

		// 获取现有明细
		List<StocktakeOrderItem> existingItems = stocktakeItemService.getByStocktakeOrderId(dto.getStocktakeId());
		Map<Long, StocktakeOrderItem> itemMap = existingItems.stream()
				.collect(Collectors.toMap(StocktakeOrderItem::getId, Function.identity()));

		// 更新明细
		for (StocktakeItemInputDTO input : dto.getItems()) {
			StocktakeOrderItem item = itemMap.get(input.getItemId());
			Assert.notNull(item, "盘点明细不存在，ID: " + input.getItemId());
			if (item.getLocationTaskId() != null) {
				StocktakeLocationTask task = stocktakeLocationTaskService.getById(item.getLocationTaskId());
				Assert.isTrue(task != null && !StocktakeTaskStatus.COMPLETED.name().equals(task.getTaskStatus()),
						"已完成库位不能修改盘点数量");
			}

			if (input.getActualQuantity() != null) {
				// 录入实盘数量
				Assert.isTrue(input.getActualQuantity() >= 0, "实盘数量不能为负数");
				item.setActualQuantity(input.getActualQuantity());
				item.setDiffQuantity(input.getActualQuantity() - item.getSystemQuantity());
				item.setStocktakeStatus(StocktakeItemStatus.COUNTED.name());
			} else {
				// 清除实盘数量
				item.setActualQuantity(null);
				item.setDiffQuantity(null);
				item.setStocktakeStatus(StocktakeItemStatus.PENDING.name());
			}

			stocktakeItemService.updateById(item);
		}

		log.info("Saved stocktake items, stocktakeId={}, itemCount={}", dto.getStocktakeId(), dto.getItems().size());
	}

	/**
	 * 确认盘点 - 触发 STOCKTAKE 过账（合并盘盈盘亏）
	 *
	 * @param id 盘点单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void confirm(Long id) {
		assertPlatform();
		StocktakeOrder order = this.getById(id);
		Assert.notNull(order, "盘点单不存在");

		boolean locationBased = order.getStocktakeMode() != null;
		String requiredStatus = locationBased ? StocktakeStatus.REVIEWING.name() : StocktakeStatus.COUNTING.name();
		Assert.isTrue(requiredStatus.equals(order.getOrderStatus()),
				locationBased ? "只有待复核状态的盘点单可以确认" : "只有盘点中状态的盘点单可以确认");

		List<StocktakeOrderItem> items = stocktakeItemService.getByStocktakeOrderId(id);
		Assert.isTrue(items.stream().allMatch(item -> item.getActualQuantity() != null), "仍有盘点明细未完成");
		List<StockPostingItemDTO> postingItems = locationBased
				? applyLocationStocktake(order, items)
				: postLegacyStocktake(order, items);

		// 更新状态为已确认
		order.setOrderStatus(StocktakeStatus.CONFIRMED.name());
		order.setConfirmTime(LocalDateTime.now());
		Long currentUserId = principalAttributeAccessor.getUserId();
		order.setConfirmBy(currentUserId);
		this.updateById(order);

		long gainCount = postingItems.stream().filter(i -> i.getDirection() == StockDirection.IN).count();
		long lossCount = postingItems.stream().filter(i -> i.getDirection() == StockDirection.OUT).count();
		log.info("Confirmed stocktake, id={}, stocktakeNo={}, gainCount={}, lossCount={}",
				order.getId(), order.getStocktakeNo(), gainCount, lossCount);
	}

	private List<StockPostingItemDTO> postLegacyStocktake(StocktakeOrder order, List<StocktakeOrderItem> items) {
		List<StockPostingItemDTO> postingItems = buildPostingItems(order, items);
		Assert.isTrue(postingItems.isEmpty(),
				"旧版盘点不能确认库存差异，请使用按库位盘点，以确保库存具有可拣货的库位和批次");
		return postingItems;
	}

	private List<StockPostingItemDTO> applyLocationStocktake(StocktakeOrder order, List<StocktakeOrderItem> items) {
		validateLocationSnapshot(order, items);
		List<StocktakeOrderItem> diffItems = items.stream()
				.filter(item -> item.getDiffQuantity() != null && item.getDiffQuantity() != 0)
				.collect(Collectors.toList());
		List<StockPostingItemDTO> postingItems = buildPostingItems(order, diffItems);
		Set<String> affectedKeys = diffItems.stream().map(this::inventoryKey)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		// First align the query snapshot with the physical source before recording the delta.
		refreshAffectedSnapshot(order.getWarehouseId(), affectedKeys);
		postStocktake(order, postingItems);

		for (StocktakeOrderItem item : diffItems) {
			if (item.getPhysicalInventoryId() != null) {
				WmsPhysicalInventory batch = physicalInventoryMapper.selectById(item.getPhysicalInventoryId());
				Assert.notNull(batch, "物理库存批次不存在，盘点明细ID=" + item.getId());
				Assert.isTrue(batch.getWarehouseId().equals(order.getWarehouseId())
						&& batch.getErpTenantId().equals(item.getErpTenantId())
						&& batch.getSkuCode().equals(item.getSkuCode())
						&& batch.getLocationCode().equals(item.getLocationCode()), "物理库存批次与盘点快照不一致");
				Assert.isTrue(nz(batch.getQuantity()) == item.getSystemQuantity(), "盘点期间库存已变化，请取消后重新盘点");
				Assert.isTrue(item.getActualQuantity() >= nz(batch.getReservedQty()),
						"实盘数量不能小于已预留数量，SKU=" + item.getSkuCode());
				batch.setQuantity(item.getActualQuantity());
				Assert.isTrue(physicalInventoryMapper.updateById(batch) == 1, "物理库存并发更新失败，请重试");
			}
			else {
				insertStocktakeBatch(order, item);
			}
		}
		refreshAffectedSnapshot(order.getWarehouseId(), affectedKeys);
		return postingItems;
	}

	private void validateLocationSnapshot(StocktakeOrder order, List<StocktakeOrderItem> items) {
		Map<Long, StocktakeOrderItem> captured = items.stream()
				.filter(item -> item.getPhysicalInventoryId() != null)
				.collect(Collectors.toMap(StocktakeOrderItem::getPhysicalInventoryId, Function.identity()));
		for (StocktakeOrderItem item : captured.values()) {
			WmsPhysicalInventory batch = physicalInventoryMapper.selectById(item.getPhysicalInventoryId());
			Assert.notNull(batch, "盘点期间物理库存批次已被删除，请取消后重新盘点");
			Assert.isTrue(Objects.equals(batch.getWarehouseId(), order.getWarehouseId())
					&& Objects.equals(batch.getErpTenantId(), item.getErpTenantId())
					&& Objects.equals(batch.getSkuCode(), item.getSkuCode())
					&& Objects.equals(batch.getLocationCode(), item.getLocationCode()),
					"盘点期间物理库存批次归属已变化，请取消后重新盘点");
			Assert.isTrue(nz(batch.getQuantity()) == nz(item.getSystemQuantity())
					&& nz(batch.getReservedQty()) == nz(item.getReservedQuantity()),
					"盘点期间库存或预留数量已变化，请取消后重新盘点");
		}

		Set<String> taskLocations = stocktakeLocationTaskService.listByStocktakeId(order.getId()).stream()
				.map(StocktakeLocationTask::getLocationCode).collect(Collectors.toSet());
		for (WmsPhysicalInventory batch : physicalInventoryMapper.listByWarehouse(order.getWarehouseId())) {
			if (!taskLocations.contains(batch.getLocationCode()) || !isBatchInOrderScope(order, batch)) {
				continue;
			}
			if ((nz(batch.getQuantity()) > 0 || nz(batch.getReservedQty()) > 0) && !captured.containsKey(batch.getId())) {
				throw new IllegalStateException("盘点期间范围内出现新库存批次，请取消后重新盘点，SKU=" + batch.getSkuCode());
			}
		}
	}

	private boolean isBatchInOrderScope(StocktakeOrder order, WmsPhysicalInventory batch) {
		if (!StocktakeMode.SPECIAL.name().equals(order.getStocktakeMode())) {
			return true;
		}
		try {
			JsonNode scope = objectMapper.readTree(order.getScopeConfig());
			JsonNode ownerNode = scope.get("specialOwnerId");
			if (ownerNode != null && !ownerNode.isNull() && ownerNode.asLong() != batch.getErpTenantId()) {
				return false;
			}
			JsonNode skuNodes = scope.get("specialSkuCodes");
			if (skuNodes != null && skuNodes.isArray() && skuNodes.size() > 0) {
				for (JsonNode skuNode : skuNodes) {
					if (batch.getSkuCode().equals(skuNode.asText())) {
						return true;
					}
				}
				return false;
			}
			return true;
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("盘点范围配置无法读取", ex);
		}
	}

	private void insertStocktakeBatch(StocktakeOrder order, StocktakeOrderItem item) {
		Assert.isTrue(item.getActualQuantity() != null && item.getActualQuantity() > 0, "账外商品实盘数量必须大于0");
		WmsZone zone = item.getZoneId() == null ? null : wmsZoneService.getById(item.getZoneId());
		WmsPhysicalInventory batch = new WmsPhysicalInventory();
		batch.setWmsTenantId(0L);
		batch.setErpTenantId(item.getErpTenantId());
		batch.setWarehouseId(order.getWarehouseId());
		batch.setSkuCode(item.getSkuCode());
		batch.setInboundItemId(0L);
		LocalDate inboundDate = item.getInboundDate() == null ? order.getStocktakeDate() : item.getInboundDate();
		batch.setInboundDate(inboundDate);
		batch.setPickOrder(physicalInventoryMapper.countSameDay(batch.getWmsTenantId(), batch.getErpTenantId(),
				batch.getWarehouseId(), batch.getSkuCode(), inboundDate) + 1);
		batch.setQuantity(item.getActualQuantity());
		batch.setReservedQty(0);
		batch.setQuality(item.getQuality() == null ? "GOOD" : item.getQuality());
		batch.setLocationCode(item.getLocationCode());
		batch.setZoneId(item.getZoneId());
		batch.setAllocatable("GOOD".equalsIgnoreCase(batch.getQuality()) && zone != null
				&& Integer.valueOf(1).equals(zone.getAllocatable()) ? 1 : 0);
		batch.setContainerStored(0);
		batch.setVersion(0);
		physicalInventoryMapper.insert(batch);
		item.setPhysicalInventoryId(batch.getId());
		item.setPickOrder(batch.getPickOrder());
		stocktakeItemService.updateById(item);
	}

	private List<StockPostingItemDTO> buildPostingItems(StocktakeOrder order, List<StocktakeOrderItem> items) {
		Map<String, Integer> netDeltas = new LinkedHashMap<>();
		Map<String, StocktakeOrderItem> samples = new LinkedHashMap<>();
		for (StocktakeOrderItem item : items) {
			if (item.getDiffQuantity() == null || item.getDiffQuantity() == 0) {
				continue;
			}
			if (!"DAMAGED".equalsIgnoreCase(item.getQuality()) && !Integer.valueOf(1).equals(item.getAllocatable())) {
				continue;
			}
			StockBucket bucket = "DAMAGED".equalsIgnoreCase(item.getQuality())
					? StockBucket.DAMAGED : StockBucket.AVAILABLE;
			String key = item.getErpTenantId() + "|" + item.getWmsTenantId() + "|" + item.getSkuCode() + "|" + bucket.name();
			netDeltas.merge(key, item.getDiffQuantity(), Integer::sum);
			samples.putIfAbsent(key, item);
		}
		List<StockPostingItemDTO> postingItems = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : netDeltas.entrySet()) {
			int delta = entry.getValue();
			if (delta == 0) {
				continue;
			}
			StocktakeOrderItem sample = samples.get(entry.getKey());
			StockBucket bucket = "DAMAGED".equalsIgnoreCase(sample.getQuality())
					? StockBucket.DAMAGED : StockBucket.AVAILABLE;
			postingItems.add(StockPostingItemDTO.builder()
					.warehouseId(order.getWarehouseId())
					.erpTenantId(sample.getErpTenantId())
					.wmsTenantId(sample.getWmsTenantId())
					.skuCode(sample.getSkuCode())
					.bucket(bucket)
					.direction(delta > 0 ? StockDirection.IN : StockDirection.OUT)
					.quantity(Math.abs(delta))
					.remark("库位盘点净差异")
					.build());
		}
		return postingItems;
	}

	private void postStocktake(StocktakeOrder order, List<StockPostingItemDTO> postingItems) {
		if (postingItems.isEmpty()) {
			return;
		}
		stockPostingService.post(StockPostingDTO.builder()
				.postingType(PostingType.STOCKTAKE)
				.warehouseId(order.getWarehouseId())
				.sourceType(SourceType.STOCKTAKE.name())
				.sourceId(order.getId())
				.sourceNo(order.getStocktakeNo())
				.items(postingItems)
				.build());
	}

	private String inventoryKey(StocktakeOrderItem item) {
		return item.getErpTenantId() + "|" + item.getSkuCode();
	}

	private void refreshAffectedSnapshot(Long warehouseId, Set<String> keys) {
		for (String key : keys) {
			String[] parts = key.split("\\|", 2);
			inventoryAggregator.refreshSnapshot(0L, Long.valueOf(parts[0]), warehouseId, parts[1]);
		}
	}

	/**
	 * 取消盘点
	 *
	 * @param id 盘点单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void cancel(Long id) {
		assertPlatform();
		StocktakeOrder order = this.getById(id);
		Assert.notNull(order, "盘点单不存在");

		Assert.isTrue(StocktakeStatus.COUNTING.name().equals(order.getOrderStatus())
				|| StocktakeStatus.REVIEWING.name().equals(order.getOrderStatus()),
				"只有盘点中或待复核状态的盘点单可以取消");

		// 更新状态为已取消
		order.setOrderStatus(StocktakeStatus.CANCELLED.name());
		this.updateById(order);

		log.info("Cancelled stocktake, id={}, stocktakeNo={}", order.getId(), order.getStocktakeNo());
	}

	/**
	 * 删除盘点单
	 *
	 * @param ids 盘点单ID列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public void delete(List<Long> ids) {
		assertPlatform();
		for (Long id : ids) {
			StocktakeOrder order = this.getById(id);
			Assert.notNull(order, "盘点单不存在，ID: " + id);

			// 只有已取消状态可以删除
			Assert.isTrue(StocktakeStatus.CANCELLED.name().equals(order.getOrderStatus()),
					"只有已取消状态的盘点单可以删除");

			// 删除明细
			stocktakeItemService.deleteByStocktakeOrderId(id);
			stocktakeLocationTaskService.deleteByStocktakeId(id);

			// 删除盘点单
			this.removeById(id);

			log.info("Deleted stocktake, id={}, stocktakeNo={}", order.getId(), order.getStocktakeNo());
		}
	}

	/**
	 * 获取差异预览（确认盘点前调用）
	 *
	 * @param id 盘点单ID
	 * @return 差异预览
	 */
	public StocktakeDiffPreviewVO getDiffPreview(Long id) {
		assertPlatform();
		StocktakeOrder order = this.getById(id);
		Assert.notNull(order, "盘点单不存在");
		Assert.isTrue(StocktakeStatus.COUNTING.name().equals(order.getOrderStatus())
				|| StocktakeStatus.REVIEWING.name().equals(order.getOrderStatus()),
				"只有盘点中或待复核状态可以预览差异");

		// 获取所有盘点明细
		List<StocktakeOrderItem> items = stocktakeItemService.getByStocktakeOrderId(id);

		StocktakeDiffPreviewVO preview = new StocktakeDiffPreviewVO();
		preview.setTotalCount(items.size());

		int noDiffCount = 0;
		int profitCount = 0;
		int profitQuantity = 0;
		int lossCount = 0;
		int lossQuantity = 0;
		int pendingCount = 0;
		List<StocktakeDiffItemVO> diffItems = new ArrayList<>();

		for (StocktakeOrderItem item : items) {
			// 未盘点的项
			if (item.getActualQuantity() == null) {
				pendingCount++;
				continue;
			}

			int diff = item.getActualQuantity() - item.getSystemQuantity();
			if (diff == 0) {
				noDiffCount++;
			} else {
				StocktakeDiffItemVO diffItem = new StocktakeDiffItemVO();
				diffItem.setSkuCode(item.getSkuCode());
				diffItem.setSystemQuantity(item.getSystemQuantity());
				diffItem.setActualQuantity(item.getActualQuantity());
				diffItem.setDiffQuantity(diff);

				if (diff > 0) {
					// 盘盈
					profitCount++;
					profitQuantity += diff;
					diffItem.setDiffType("PROFIT");
				} else {
					// 盘亏
					lossCount++;
					lossQuantity += Math.abs(diff);
					diffItem.setDiffType("LOSS");
				}
				diffItems.add(diffItem);
			}
		}

		// 批量填充SKU信息
		skuBriefService.enrichForQuery(diffItems, StocktakeDiffItemVO::getSkuCode, StocktakeDiffItemVO::setSkuBrief);

		preview.setNoDiffCount(noDiffCount);
		preview.setProfitCount(profitCount);
		preview.setProfitQuantity(profitQuantity);
		preview.setLossCount(lossCount);
		preview.setLossQuantity(lossQuantity);
		preview.setPendingCount(pendingCount);
		preview.setDiffItems(diffItems);

		return preview;
	}

	/**
	 * 获取仓库可盘点SKU预览
	 *
	 * @param warehouseId 仓库ID
	 * @return 预览信息
	 */
	public AvailableSkuPreviewVO getAvailableSkuPreview(Long warehouseId) {
		assertPlatform();
		// 查询该仓库下所有有记录的库存（含数量为0的）
		List<Inventory> inventories = inventoryService.getAllByWarehouseId(warehouseId);

		AvailableSkuPreviewVO preview = new AvailableSkuPreviewVO();
		preview.setTotalCount(inventories.size());

		// 取前10条作为预览
		List<Inventory> previewInventories = inventories.stream()
				.limit(10)
				.collect(Collectors.toList());

		if (previewInventories.isEmpty()) {
			preview.setItems(new ArrayList<>());
			return preview;
		}

		// 组装VO
		List<AvailableSkuVO> items = previewInventories.stream().map(inv -> {
			AvailableSkuVO vo = new AvailableSkuVO();
			vo.setSkuCode(inv.getSkuCode());
			vo.setStockQuantity(inv.getAvailableQuantity() + inv.getReservedQuantity());
			return vo;
		}).collect(Collectors.toList());

		// 填充 SKU 展示信息
		skuBriefService.enrichForQuery(items, AvailableSkuVO::getSkuCode, AvailableSkuVO::setSkuBrief);

		preview.setItems(items);
		return preview;
	}

	/**
	 * 批量将未盘点项标记为无差异（实盘=系统）
	 *
	 * @param id 盘点单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void batchMarkNoDiff(Long id) {
		assertPlatform();
		StocktakeOrder order = this.getById(id);
		Assert.notNull(order, "盘点单不存在");
		Assert.isTrue(StocktakeStatus.COUNTING.name().equals(order.getOrderStatus()),
				"只有盘点中状态的盘点单可以操作");

		// 获取所有未盘点的明细
		List<StocktakeOrderItem> allItems = stocktakeItemService.getByStocktakeOrderId(id);
		List<StocktakeOrderItem> pendingItems = allItems.stream()
				.filter(item -> StocktakeItemStatus.PENDING.name().equals(item.getStocktakeStatus()))
				.collect(Collectors.toList());

		if (pendingItems.isEmpty()) {
			log.info("No pending items to mark, stocktakeId={}", id);
			return;
		}

		// 批量更新：实盘数量=系统数量，差异=0，状态=已盘点
		for (StocktakeOrderItem item : pendingItems) {
			item.setActualQuantity(item.getSystemQuantity());
			item.setDiffQuantity(0);
			item.setStocktakeStatus(StocktakeItemStatus.COUNTED.name());
		}
		stocktakeItemService.updateBatchById(pendingItems);

		log.info("Batch marked no diff, stocktakeId={}, count={}", id, pendingItems.size());
	}

	/**
	 * 保存盘点单（带重试机制处理单号冲突）
	 *
	 * @param order 盘点单实体
	 */
	private void saveOrderWithRetry(StocktakeOrder order) {
		for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
			try {
				this.save(order);
				return;
			} catch (DuplicateKeyException e) {
				if (attempt == MAX_RETRY_ATTEMPTS) {
					log.error("Failed to save stocktake order after {} attempts, stocktakeNo={}",
							MAX_RETRY_ATTEMPTS, order.getStocktakeNo());
					throw new IllegalStateException("盘点单号生成失败，请重试", e);
				}
				// 重新生成单号并重试
				String newNo = generateStocktakeNo();
				log.warn("Duplicate stocktake no detected, retrying with new no: {} -> {}, attempt={}",
						order.getStocktakeNo(), newNo, attempt);
				order.setStocktakeNo(newNo);
				order.setId(null); // 清除可能已设置的ID
			}
		}
	}

	/**
	 * 追加盘点SKU
	 * 系统自动判断来源类型：有库存记录→库内，无→追加
	 *
	 * @param dto 追加请求
	 * @return 新增的明细列表（含SKU展示信息）
	 */
	@Transactional(rollbackFor = Exception.class)
	public List<StocktakeItemVO> addSkus(StocktakeAddSkuDTO dto) {
		assertPlatform();
		StocktakeOrder order = this.getById(dto.getStocktakeId());
		Assert.notNull(order, "盘点单不存在");
		Assert.isTrue(StocktakeStatus.COUNTING.name().equals(order.getOrderStatus()),
				"只有盘点中状态的盘点单可以追加SKU");

		// 获取已存在的 SKU 编码
		List<StocktakeOrderItem> existingItems = stocktakeItemService.getByStocktakeOrderId(dto.getStocktakeId());
		Set<String> existingSkuCodes = existingItems.stream()
				.map(StocktakeOrderItem::getSkuCode)
				.collect(Collectors.toSet());

		// 过滤已存在的
		List<String> newSkuCodes = dto.getSkuCodes().stream()
				.filter(code -> !existingSkuCodes.contains(code))
				.collect(Collectors.toList());

		if (newSkuCodes.isEmpty()) {
			return new ArrayList<>();
		}

		// 查询 SKU 信息（SKU 目录按货主 tenant_id 隔离；平台端跨货主追加时按所选货主切上下文查）
		List<Sku> skus = lookupSkusByOwner(dto.getErpTenantId(), newSkuCodes);
		Map<String, Sku> skuMap = skus.stream()
				.collect(Collectors.toMap(Sku::getSkuCode, Function.identity()));

		// 查询库存信息（按追加所指定货主），用于自动判断来源类型
		Map<String, Inventory> inventoryMap = inventoryService.getStockMapByErpWarehouseAndSkuCodes(
				dto.getErpTenantId(), order.getWarehouseId(), newSkuCodes);

		// 构建明细
		List<StocktakeOrderItem> items = new ArrayList<>();
		for (String skuCode : newSkuCodes) {
			Sku sku = skuMap.get(skuCode);
			if (sku == null) {
				continue;
			}

			StocktakeOrderItem item = new StocktakeOrderItem();
			item.setErpTenantId(dto.getErpTenantId());
			item.setSkuCode(skuCode);
			item.setStocktakeStatus(StocktakeItemStatus.PENDING.name());

			// 自动判断来源类型：有库存记录→库内，无→追加
			Inventory inv = inventoryMap.get(skuCode);
			if (inv != null) {
				// 有库存记录，视为库内SKU
				item.setSourceType(StocktakeItemSource.EXISTING.name());
				item.setSystemQuantity(inv.getAvailableQuantity() + inv.getReservedQuantity());
			} else {
				// 无库存记录，视为追加SKU
				item.setSourceType(StocktakeItemSource.ADDED.name());
				item.setSystemQuantity(0);
			}

			items.add(item);
		}

		// 批量保存并返回新增的明细
		if (!items.isEmpty()) {
			stocktakeItemService.batchSave(dto.getStocktakeId(), items);
			log.info("Added SKUs to stocktake, stocktakeId={}, count={}", dto.getStocktakeId(), items.size());

			// 转换为 VO 并填充 SKU 展示信息
			return stocktakeItemService.convertToVoList(items);
		}

		return new ArrayList<>();
	}

	/**
	 * 按货主查 SKU 目录：平台端跨货主追加时，按所选货主 tenant_id 临时切上下文查其 SKU；
	 * 非平台身份走当前租户（自己的目录），防越权。
	 * @param erpTenantId 追加所指定的货主ID
	 * @param skuCodes    SKU 编码
	 * @return SKU 列表
	 */
	private List<Sku> lookupSkusByOwner(Long erpTenantId, List<String> skuCodes) {
		String type = tenantIdentityService.currentIdentity(null).getIdentityType();
		if (erpTenantId != null && TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(type)) {
			return TenantContext.runAs(erpTenantId, () -> skuService.listBySkuCodes(skuCodes));
		}
		return skuService.listBySkuCodes(skuCodes);
	}

	/**
	 * 删除盘点明细
	 *
	 * @param stocktakeId 盘点单ID
	 * @param itemIds     明细ID列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public void removeItems(Long stocktakeId, List<Long> itemIds) {
		assertPlatform();
		StocktakeOrder order = this.getById(stocktakeId);
		Assert.notNull(order, "盘点单不存在");
		Assert.isTrue(StocktakeStatus.COUNTING.name().equals(order.getOrderStatus()),
				"只有盘点中状态的盘点单可以删除明细");

		stocktakeItemService.removeByIds(itemIds);
		log.info("Removed stocktake items, stocktakeId={}, itemIds={}", stocktakeId, itemIds);
	}

	/**
	 * 获取仓库内可选择的SKU列表（用于部分盘点选择）
	 *
	 * @param warehouseId 仓库ID
	 * @param stocktakeId 盘点单ID（用于排除已添加的）
	 * @return SKU列表
	 */
	public List<AvailableSkuVO> getSelectableSkus(Long warehouseId, Long stocktakeId) {
		assertPlatform();
		// 获取仓库所有有记录的库存
		List<Inventory> inventories = inventoryService.getAllByWarehouseId(warehouseId);

		// 获取已添加的 SKU
		Set<String> addedSkuCodes = new HashSet<>();
		if (stocktakeId != null) {
			List<StocktakeOrderItem> items = stocktakeItemService.getByStocktakeOrderId(stocktakeId);
			addedSkuCodes = items.stream()
					.map(StocktakeOrderItem::getSkuCode)
					.collect(Collectors.toSet());
		}

		// 过滤已添加的
		final Set<String> finalAddedCodes = addedSkuCodes;
		List<Inventory> available = inventories.stream()
				.filter(inv -> !finalAddedCodes.contains(inv.getSkuCode()))
				.collect(Collectors.toList());

		if (available.isEmpty()) {
			return new ArrayList<>();
		}

		// 组装 VO
		List<AvailableSkuVO> result = available.stream().map(inv -> {
			AvailableSkuVO vo = new AvailableSkuVO();
			vo.setSkuCode(inv.getSkuCode());
			vo.setStockQuantity(inv.getAvailableQuantity() + inv.getReservedQuantity());
			return vo;
		}).collect(Collectors.toList());

		// 填充 SKU 展示信息
		skuBriefService.enrichForQuery(result, AvailableSkuVO::getSkuCode, AvailableSkuVO::setSkuBrief);

		return result;
	}

}
