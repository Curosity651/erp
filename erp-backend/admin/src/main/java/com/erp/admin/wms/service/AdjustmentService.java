package com.erp.admin.wms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.AdjustmentMapper;
import com.erp.admin.wms.mapper.WmsLocationInventoryMapper;
import com.erp.admin.wms.model.dto.AdjustmentDTO;
import com.erp.admin.wms.model.dto.AdjustmentItemDTO;
import com.erp.admin.wms.model.entity.AdjustmentOrder;
import com.erp.admin.wms.model.entity.AdjustmentOrderItem;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.entity.WmsLocationInventory;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.enums.AdjustmentStatus;
import com.erp.admin.wms.model.enums.AdjustmentType;
import com.erp.admin.wms.model.qo.AdjustmentQO;
import com.erp.admin.wms.model.vo.AdjustmentDetailVO;
import com.erp.admin.wms.model.vo.AdjustmentItemVO;
import com.erp.admin.wms.model.vo.AdjustmentPageVO;
import com.erp.admin.wms.model.vo.AdjustmentStatsVO;
import com.erp.admin.wms.model.vo.PalletSummaryVO;
import com.erp.admin.wms.model.vo.ScrapBatchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 报废单服务（原「库存调整单」收敛为仅「报废」，并改为货主审批流）。
 *
 * <p>流程：<b>平台</b>发起报废(选货主+仓+具体批次+数量)→发起即<b>冻结</b>待报废批次(reserved_qty)
 * →状态 PENDING_OWNER 待货主确认；<b>货主</b>同意→PENDING_DESTROY；
 * <b>海外仓</b>确认实际销毁→真正扣减批次(quantity)、写 SCRAP 流水→SCRAPPED；
 * 货主驳回或平台撤销→释放冻结→REJECTED / CANCELLED。批次真源操作见 {@link WmsPhysicalInventoryService}。
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdjustmentService extends ExtendServiceImpl<AdjustmentMapper, AdjustmentOrder> {

	/** 单号生成最大重试次数 */
	private static final int MAX_RETRY_ATTEMPTS = 3;

	private final AdjustmentItemService adjustmentItemService;

	private final WarehouseService warehouseService;

	private final WmsPhysicalInventoryService physicalInventoryService;

	private final WmsLocationInventoryMapper locationInventoryMapper;

	private final LocationInventoryService locationInventoryService;

	private final WmsLocationService wmsLocationService;

	private final WmsZoneService wmsZoneService;

	private final WmsPalletService palletService;

	private final TenantIdentityService tenantIdentityService;

	private final PrincipalAttributeAccessor principalAttributeAccessor;

	private final WarehouseSkuCodeService warehouseSkuCodeService;

	// ==================== 查询 ====================

	/**
	 * 分页查询。货主端强制只看自己的报废单；平台端看全部（可按条件收窄）。
	 */
	public PageResult<AdjustmentPageVO> queryPage(PageParam pageParam, AdjustmentQO qo) {
		scopeToOwnerIfErpUser(qo);

		IPage<AdjustmentPageVO> page = PageUtil.prodPage(pageParam);
		baseMapper.queryPage(page, qo);

		List<AdjustmentPageVO> records = page.getRecords();
		if (records.isEmpty()) {
			return new PageResult<>(records, page.getTotal());
		}

		List<Long> ids = records.stream().map(AdjustmentPageVO::getId).collect(Collectors.toList());
		Map<Long, AdjustmentStatsVO> statsMap = baseMapper.selectStatsByIds(ids)
				.stream()
				.collect(Collectors.toMap(AdjustmentStatsVO::getAdjustmentOrderId, Function.identity()));

		for (AdjustmentPageVO vo : records) {
			AdjustmentStatsVO stats = statsMap.get(vo.getId());
			vo.setSkuCount(stats != null ? stats.getSkuCount() : 0);
			vo.setTotalQuantity(stats != null ? stats.getTotalQuantity() : 0);
		}

		return new PageResult<>(records, page.getTotal());
	}

	/**
	 * 报废单详情。货主端只能看自己的。
	 */
	public AdjustmentDetailVO getDetail(Long id) {
		AdjustmentDetailVO detail = baseMapper.selectDetailById(id);
		Assert.notNull(detail, "报废单不存在");

		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		if (TenantIdentityService.IDENTITY_ERP_USER.equals(identity.getIdentityType())) {
			Assert.isTrue(identity.getTenantId() != null && identity.getTenantId().equals(detail.getErpTenantId()),
					"无权查看该报废单");
		}

		List<AdjustmentStatsVO> statsVOS = baseMapper.selectStatsByIds(Collections.singleton(id));
		if (!statsVOS.isEmpty()) {
			AdjustmentStatsVO s = statsVOS.get(0);
			detail.setSkuCount(s.getSkuCount());
			detail.setTotalQuantity(s.getTotalQuantity());
		}

		List<AdjustmentItemVO> items = adjustmentItemService.getVoListByAdjustmentOrderId(id);
		items.forEach(item -> item.setWarehouseSkuCode(
				warehouseSkuCodeService.build(detail.getErpTenantId(), item.getSkuCode())));
		detail.setItems(items);
		return detail;
	}

	public List<ScrapBatchVO> listScrapCandidates(Long erpTenantId, Long warehouseId) {
		assertPlatform();
		Assert.notNull(erpTenantId, "货主不能为空");
		Assert.notNull(warehouseId, "仓库不能为空");
		warehouseService.validateOperableOwnWarehouse(warehouseId);
		Map<Long, WmsLocation> locations = wmsLocationService.listByWarehouse(warehouseId).stream()
				.collect(Collectors.toMap(WmsLocation::getId, Function.identity()));
		Map<Long, String> zoneTypes = wmsZoneService.listByWarehouse(warehouseId).stream()
				.collect(Collectors.toMap(WmsZone::getId, WmsZone::getZoneType, (left, right) -> left));
		List<WmsLocationInventory> inventories = locationInventoryMapper.selectList(
				Wrappers.<WmsLocationInventory>lambdaQuery()
						.eq(WmsLocationInventory::getErpTenantId, erpTenantId)
						.eq(WmsLocationInventory::getWarehouseId, warehouseId)
						.gt(WmsLocationInventory::getQuantity, 0));
		List<ScrapBatchVO> result = new ArrayList<>();
		for (WmsLocationInventory inventory : inventories) {
			WmsLocation location = locations.get(inventory.getLocationId());
			if (location == null || !"DEFECTIVE".equals(zoneTypes.get(location.getZoneId()))
					|| !("DEFECTIVE".equals(inventory.getQuality()) || "DAMAGED".equals(inventory.getQuality()))) {
				continue;
			}
			ScrapBatchVO vo = new ScrapBatchVO();
			vo.setId(inventory.getId());
			vo.setSourceInventoryId(inventory.getId());
			vo.setSkuCode(inventory.getSkuCode());
			vo.setWarehouseSkuCode(warehouseSkuCodeService.build(
					erpTenantId, inventory.getSkuCode()));
			vo.setLocationCode(location.getLocationCode());
			vo.setQuality(inventory.getQuality());
			vo.setQuantity(inventory.getQuantity());
			vo.setReservedQty(inventory.getReservedQuantity());
			vo.setZoneId(location.getZoneId());
			result.add(vo);
		}
		result.sort(Comparator.comparing(ScrapBatchVO::getLocationCode,
				Comparator.nullsLast(WmsPalletService::compareNatural))
				.thenComparing(ScrapBatchVO::getSkuCode, Comparator.nullsLast(String::compareToIgnoreCase)));
		return result;
	}

	/**
	 * 生成报废单号：AD + yyyyMMdd + 4 位序号。
	 */
	public String generateAdjustmentNo() {
		String prefix = "AD" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		int count = baseMapper.countTodayOrders(prefix);
		return prefix + String.format("%04d", count + 1);
	}

	// ==================== 平台：发起 / 撤销 ====================

	/**
	 * 平台发起报废：校验批次归属与可用 → 建单(PENDING_OWNER) → 冻结待报废批次。
	 * @param dto 报废单DTO（items 每行锁定一个具体批次 physicalInventoryId + 数量）
	 * @return 报废单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long create(AdjustmentDTO dto) {
		assertPlatform();
		Assert.notEmpty(dto.getItems(), "报废明细不能为空");
		Assert.notNull(dto.getErpTenantId(), "货主不能为空");
		Assert.notNull(dto.getWarehouseId(), "仓库不能为空");
		warehouseService.validateOperableOwnWarehouse(dto.getWarehouseId());

		// 加载并校验批次（存在 / 属本仓本货主 / 可用充足），构建明细
		List<AdjustmentOrderItem> items = new ArrayList<>();
		for (AdjustmentItemDTO d : dto.getItems()) {
			Assert.notNull(d.getSourceInventoryId(), "目标库存不能为空");
			WmsLocationInventory inventory = locationInventoryMapper.selectForUpdate(d.getSourceInventoryId());
			Assert.notNull(inventory, "库存不存在：" + d.getSourceInventoryId());
			Assert.isTrue(dto.getWarehouseId().equals(inventory.getWarehouseId()), "库存不属于所选仓库");
			Assert.isTrue(dto.getErpTenantId().equals(inventory.getErpTenantId()), "库存不属于所选货主");
			WmsLocation location = wmsLocationService.getById(inventory.getLocationId());
			Assert.notNull(location, "库存库位不存在");
			WmsZone zone = wmsZoneService.getById(location.getZoneId());
			validateScrapCandidate(inventory.getQuality(), zone == null ? null : zone.getZoneType());
			int reserved = inventory.getReservedQuantity() == null ? 0 : inventory.getReservedQuantity();
			int available = inventory.getQuantity() - reserved;
			Assert.isTrue(d.getQuantity() != null && d.getQuantity() > 0 && d.getQuantity() <= available,
					String.format("库位[%s]报废数量(%s)超过可用(%d)", location.getLocationCode(), d.getQuantity(), available));

			AdjustmentOrderItem item = new AdjustmentOrderItem();
			item.setSourceInventoryId(inventory.getId());
			item.setErpTenantId(dto.getErpTenantId());
			item.setSkuCode(inventory.getSkuCode());
			item.setLocationCode(location.getLocationCode());
			item.setQuality(inventory.getQuality());
			item.setQuantity(d.getQuantity());
			item.setRemark(d.getRemark());
			items.add(item);
		}

		AdjustmentOrder order = new AdjustmentOrder();
		order.setWarehouseId(dto.getWarehouseId());
		order.setErpTenantId(dto.getErpTenantId());
		order.setAdjustmentType(AdjustmentType.SCRAP.name());
		order.setAdjustmentDate(dto.getAdjustmentDate() != null ? dto.getAdjustmentDate() : LocalDate.now());
		order.setAdjustmentReason(dto.getAdjustmentReason());
		order.setAdjustmentNo(generateAdjustmentNo());
		order.setOrderStatus(AdjustmentStatus.PENDING_OWNER.name());
		order.setCreateBy(currentUserId());
		saveOrderWithRetry(order);

		for (AdjustmentOrderItem item : items) {
			item.setAdjustmentOrderId(order.getId());
		}
		adjustmentItemService.saveBatch(items);

		// 冻结待报废批次（同事务；任一不足则整单回滚）
		for (AdjustmentOrderItem item : items) {
			locationInventoryService.reserveInventory(item.getSourceInventoryId(), item.getQuantity(),
					scrapContext(order, item, com.erp.admin.wms.model.enums.InventoryEventType.SCRAP_RESERVE,
							"报废库存预留"));
		}

		log.info("平台发起报废, id={}, no={}, erpTenantId={}, items={}", order.getId(), order.getAdjustmentNo(),
				order.getErpTenantId(), items.size());
		return order.getId();
	}

	/**
	 * 平台撤销（仅待货主确认可撤销）：释放冻结 → CANCELLED。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void cancel(Long id) {
		assertPlatform();
		AdjustmentOrder order = requireOrder(id);
		Assert.isTrue(AdjustmentStatus.PENDING_OWNER.name().equals(order.getOrderStatus()),
				"只有待货主确认的报废单可以撤销");
		releaseAll(id);
		if (baseMapper.casStatus(id, AdjustmentStatus.PENDING_OWNER.name(),
				AdjustmentStatus.CANCELLED.name()) != 1) {
			throw new BusinessException(409, "报废单状态已变化，请刷新后重试");
		}
		order.setOrderStatus(AdjustmentStatus.CANCELLED.name());
		this.updateById(order);
		log.info("平台撤销报废, id={}, no={}", order.getId(), order.getAdjustmentNo());
	}

	// ==================== 货主：确认 / 驳回 ====================

	/**
	 * 货主同意报废（仅待货主确认，且为本货主）：只推进审批状态，不扣减库存。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void ownerConfirm(Long id) {
		AdjustmentOrder order = requireOrder(id);
		assertOwnerOf(order);
		Assert.isTrue(AdjustmentStatus.PENDING_OWNER.name().equals(order.getOrderStatus()),
				"只有待货主确认的报废单可以确认");

		if (baseMapper.casStatus(id, AdjustmentStatus.PENDING_OWNER.name(),
				AdjustmentStatus.PENDING_DESTROY.name()) != 1) {
			throw new BusinessException(409, "报废单状态已变化，请刷新后重试");
		}

		LocalDateTime now = LocalDateTime.now();
		Long uid = currentUserId();
		order.setOrderStatus(AdjustmentStatus.PENDING_DESTROY.name());
		order.setOwnerActionTime(now);
		order.setOwnerActionBy(uid);
		this.updateById(order);
		log.info("货主同意报废, id={}, no={}", order.getId(), order.getAdjustmentNo());
	}

	@Transactional(rollbackFor = Exception.class)
	public void destroy(Long id) {
		assertPlatform();
		AdjustmentOrder order = requireOrder(id);
		Assert.isTrue(AdjustmentStatus.PENDING_DESTROY.name().equals(order.getOrderStatus()),
				"只有货主已同意的报废单可以确认销毁");
		if (baseMapper.casStatus(id, AdjustmentStatus.PENDING_DESTROY.name(),
				AdjustmentStatus.SCRAPPED.name()) != 1) {
			throw new BusinessException(409, "报废单状态已变化，请刷新后重试");
		}
		List<AdjustmentOrderItem> items = adjustmentItemService.getByAdjustmentOrderId(id);
		Assert.notEmpty(items, "报废明细为空");
		for (AdjustmentOrderItem item : items) {
			locationInventoryService.scrapReservedInventory(item.getSourceInventoryId(), item.getQuantity(),
					scrapContext(order, item, com.erp.admin.wms.model.enums.InventoryEventType.SCRAP, "报废销毁"));
		}
		LocalDateTime now = LocalDateTime.now();
		Long uid = currentUserId();
		order.setOrderStatus(AdjustmentStatus.SCRAPPED.name());
		order.setConfirmTime(now);
		order.setConfirmBy(uid);
		this.updateById(order);
		log.info("海外仓确认实际销毁, id={}, no={}", order.getId(), order.getAdjustmentNo());
	}

	/**
	 * 货主驳回（仅待货主确认，且为本货主）：释放冻结 → REJECTED。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void ownerReject(Long id, String reason) {
		AdjustmentOrder order = requireOrder(id);
		assertOwnerOf(order);
		Assert.isTrue(AdjustmentStatus.PENDING_OWNER.name().equals(order.getOrderStatus()),
				"只有待货主确认的报废单可以驳回");
		Assert.hasText(reason, "请填写驳回原因");

		releaseAll(id);
		if (baseMapper.casStatus(id, AdjustmentStatus.PENDING_OWNER.name(),
				AdjustmentStatus.REJECTED.name()) != 1) {
			throw new BusinessException(409, "报废单状态已变化，请刷新后重试");
		}
		order.setOrderStatus(AdjustmentStatus.REJECTED.name());
		order.setRejectReason(reason);
		order.setOwnerActionTime(LocalDateTime.now());
		order.setOwnerActionBy(currentUserId());
		this.updateById(order);
		log.info("货主驳回报废, id={}, no={}, reason={}", order.getId(), order.getAdjustmentNo(), reason);
	}

	// ==================== 删除 ====================

	/**
	 * 删除报废单（仅已取消/已驳回可删；已销毁需留痕不可删）。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void delete(List<Long> ids) {
		for (Long id : ids) {
			AdjustmentOrder order = requireOrder(id);
			Assert.isTrue(AdjustmentStatus.CANCELLED.name().equals(order.getOrderStatus())
							|| AdjustmentStatus.REJECTED.name().equals(order.getOrderStatus()),
					"只有已取消或已驳回的报废单可以删除");
			adjustmentItemService.deleteByAdjustmentOrderId(id);
			this.removeById(id);
			log.info("删除报废单, id={}, no={}", order.getId(), order.getAdjustmentNo());
		}
	}

	// ==================== 内部 ====================

	private void releaseAll(Long orderId) {
		AdjustmentOrder order = requireOrder(orderId);
		List<AdjustmentOrderItem> items = adjustmentItemService.getByAdjustmentOrderId(orderId);
		for (AdjustmentOrderItem item : items) {
			locationInventoryService.releaseInventory(item.getSourceInventoryId(), item.getQuantity(),
					scrapContext(order, item, com.erp.admin.wms.model.enums.InventoryEventType.SCRAP_RELEASE,
							"取消报废预留"));
		}
	}

	private com.erp.admin.wms.model.dto.InventoryMutationContext scrapContext(AdjustmentOrder order,
			AdjustmentOrderItem item, com.erp.admin.wms.model.enums.InventoryEventType eventType, String reason) {
		return com.erp.admin.wms.model.dto.InventoryMutationContext.builder().eventType(eventType)
				.sourceType("SCRAP_ORDER").sourceId(order.getId()).sourceNo(order.getAdjustmentNo())
				.operatorId(currentUserId()).reason(reason)
				.idempotencyKey("scrap:" + eventType.name() + ":" + order.getId() + ":" + item.getId()).build();
	}

	public List<PalletSummaryVO> listPrintablePallets(Long orderId) {
		AdjustmentOrder order = requireOrder(orderId);
		AdjustmentDetailVO detail = getDetail(orderId);
		Assert.isTrue(AdjustmentStatus.SCRAPPED.name().equals(detail.getOrderStatus()),
				"报废完成后才能打印更新后的托盘标签");
		return Collections.emptyList();
	}

	public static void validateScrapCandidate(String quality, String zoneType) {
		Assert.isTrue("DEFECTIVE".equalsIgnoreCase(quality) || "DAMAGED".equalsIgnoreCase(quality),
				"报废只允许选择不良品");
		Assert.isTrue("DEFECTIVE".equalsIgnoreCase(zoneType), "报废只允许选择不良品区库存");
	}

	private boolean isDefectiveZone(WmsPhysicalInventory batch, Long warehouseId) {
		if (batch.getZoneId() == null) {
			return false;
		}
		WmsZone zone = wmsZoneService.getById(batch.getZoneId());
		return zone != null && warehouseId.equals(zone.getWarehouseId())
				&& "DEFECTIVE".equals(zone.getZoneType());
	}

	private void enrichPallets(List<AdjustmentItemVO> items) {
		Map<Long, WmsPhysicalInventory> batches = new java.util.LinkedHashMap<>();
		for (AdjustmentItemVO item : items) {
			WmsPhysicalInventory batch = physicalInventoryService.getById(item.getPhysicalInventoryId());
			if (batch != null) {
				batches.put(item.getPhysicalInventoryId(), batch);
				item.setPalletId(batch.getPalletId());
				item.setSlotCode(batch.getLocationCode());
			}
		}
		Set<Long> palletIds = batches.values().stream().map(WmsPhysicalInventory::getPalletId)
				.filter(id -> id != null).collect(Collectors.toSet());
		Map<Long, PalletSummaryVO> pallets = palletService.summaries(palletIds).stream()
				.collect(Collectors.toMap(PalletSummaryVO::getId, Function.identity()));
		for (AdjustmentItemVO item : items) {
			WmsPhysicalInventory batch = batches.get(item.getPhysicalInventoryId());
			PalletSummaryVO pallet = batch == null ? null : pallets.get(batch.getPalletId());
			if (pallet != null) {
				item.setPalletNo(pallet.getPalletNo());
				item.setSlotCode(pallet.getSlotCode());
			}
		}
	}

	private AdjustmentOrder requireOrder(Long id) {
		AdjustmentOrder order = this.getById(id);
		Assert.notNull(order, "报废单不存在");
		return order;
	}

	private void assertPlatform() {
		String type = tenantIdentityService.currentIdentity(null).getIdentityType();
		if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(type)) {
			throw new BusinessException(403, "仅海外仓平台可发起/撤销报废");
		}
	}

	private void assertOwnerOf(AdjustmentOrder order) {
		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		if (!TenantIdentityService.IDENTITY_ERP_USER.equals(identity.getIdentityType())
				|| identity.getTenantId() == null || !identity.getTenantId().equals(order.getErpTenantId())) {
			throw new BusinessException(403, "仅货主本人可确认/驳回该报废单");
		}
	}

	private void scopeToOwnerIfErpUser(AdjustmentQO qo) {
		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		if (TenantIdentityService.IDENTITY_ERP_USER.equals(identity.getIdentityType())) {
			qo.setErpTenantId(identity.getTenantId());
		}
	}

	private Long currentUserId() {
		try {
			return principalAttributeAccessor.getUserId();
		}
		catch (Exception ignore) {
			return null;
		}
	}

	/**
	 * 保存报废单（带重试处理单号冲突）。
	 */
	private void saveOrderWithRetry(AdjustmentOrder order) {
		for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
			try {
				this.save(order);
				return;
			}
			catch (DuplicateKeyException e) {
				if (attempt == MAX_RETRY_ATTEMPTS) {
					throw new IllegalStateException("报废单号生成失败，请重试", e);
				}
				order.setAdjustmentNo(generateAdjustmentNo());
				order.setId(null);
			}
		}
	}

}
