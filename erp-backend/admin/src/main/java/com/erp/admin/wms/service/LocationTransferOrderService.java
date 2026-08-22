package com.erp.admin.wms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.wms.mapper.LocationTransferOrderMapper;
import com.erp.admin.wms.mapper.SalesOutboundItemMapper;
import com.erp.admin.wms.mapper.SalesOutboundMapper;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.mapper.WmsPalletMapper;
import com.erp.admin.wms.model.dto.LocationTransferBatchCreateDTO;
import com.erp.admin.wms.model.dto.LocationTransferCreateDTO;
import com.erp.admin.wms.model.dto.LocationTransferItemDTO;
import com.erp.admin.wms.model.dto.LocationTransferPlanDTO;
import com.erp.admin.wms.model.dto.LocationTransferPlanItemDTO;
import com.erp.admin.wms.model.entity.LocationTransferItem;
import com.erp.admin.wms.model.entity.LocationTransferOrder;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.entity.WmsPallet;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.enums.LocationTransferStatus;
import com.erp.admin.wms.model.enums.LocationTransferReason;
import com.erp.admin.wms.model.enums.OutboundOrderStatus;
import com.erp.admin.wms.model.qo.LocationTransferQO;
import com.erp.admin.wms.model.vo.LocationTransferDetailVO;
import com.erp.admin.wms.model.vo.LocationTransferItemVO;
import com.erp.admin.wms.model.vo.LocationTransferPageVO;
import com.erp.admin.wms.model.vo.LocationTransferStatsVO;
import com.erp.admin.wms.model.vo.PalletSlotVO;
import com.erp.admin.wms.model.vo.PalletSummaryVO;
import com.erp.admin.wms.model.vo.StockShortageVO;
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
 * 库位调整单（库内移库单）服务。
 *
 * <p>流程：<b>平台</b>新建调整单(选货主+仓+若干"源批次→目标库位+数量")→状态 PENDING 待调整，<b>不动库存</b>；
 * 点<b>调整完成</b>逐条执行移库(批次真源变更 + LOCATION_TRANSFER 流水留痕)→COMPLETED；执行前可撤销→CANCELLED。
 * 不冻结源批次：四约束与可用量在<b>执行时再次校验</b>，不满足则整单回滚。移库引擎见 {@link LocationTransferService}、
 * {@link WmsPhysicalInventoryService#locationTransfer}。
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationTransferOrderService
		extends ExtendServiceImpl<LocationTransferOrderMapper, LocationTransferOrder> {

	private static final int MAX_RETRY_ATTEMPTS = 3;

	private final LocationTransferItemService itemService;

	private final LocationTransferService locationTransferService;

	private final WarehouseService warehouseService;

	private final WmsPhysicalInventoryService physicalInventoryService;

	private final TenantIdentityService tenantIdentityService;

	private final PrincipalAttributeAccessor principalAttributeAccessor;

	private final SysTenantMapper sysTenantMapper;

	private final WmsPhysicalInventoryMapper physicalInventoryMapper;

	private final WmsPalletMapper palletMapper;

	private final WmsPalletService palletService;

	private final WmsLocationService wmsLocationService;

	private final WmsZoneService wmsZoneService;

	private final SalesOutboundMapper salesOutboundMapper;

	private final SalesOutboundItemMapper salesOutboundItemMapper;

	private final OutboundPickingService outboundPickingService;

	// ==================== 查询 ====================

	/**
	 * 分页查询（平台看全部，可按条件收窄；货主端若命中则只看自己的）。
	 */
	public PageResult<LocationTransferPageVO> queryPage(PageParam pageParam, LocationTransferQO qo) {
		scopeToOwnerIfErpUser(qo);

		IPage<LocationTransferPageVO> page = PageUtil.prodPage(pageParam);
		baseMapper.queryPage(page, qo);

		List<LocationTransferPageVO> records = page.getRecords();
		if (records.isEmpty()) {
			return new PageResult<>(records, page.getTotal());
		}

		List<Long> ids = records.stream().map(LocationTransferPageVO::getId).collect(Collectors.toList());
		Map<Long, LocationTransferStatsVO> statsMap = baseMapper.selectStatsByIds(ids)
				.stream()
				.collect(Collectors.toMap(LocationTransferStatsVO::getTransferOrderId, Function.identity()));

		for (LocationTransferPageVO vo : records) {
			LocationTransferStatsVO stats = statsMap.get(vo.getId());
			vo.setItemCount(stats != null ? stats.getItemCount() : 0);
			vo.setTotalQuantity(stats != null ? stats.getTotalQuantity() : 0);
		}
		return new PageResult<>(records, page.getTotal());
	}

	/**
	 * 详情。
	 */
	public LocationTransferDetailVO getDetail(Long id) {
		LocationTransferDetailVO detail = baseMapper.selectDetailById(id);
		Assert.notNull(detail, "库位调整单不存在");

		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		if (TenantIdentityService.IDENTITY_ERP_USER.equals(identity.getIdentityType())) {
			Assert.isTrue(identity.getTenantId() != null && identity.getTenantId().equals(detail.getErpTenantId()),
					"无权查看该库位调整单");
		}

		List<LocationTransferItemVO> items = itemService.getVoListByOrderId(id);
		Map<String, WmsLocation> locationByCode = wmsLocationService
				.listByWarehouse(detail.getWarehouseId()).stream()
				.filter(location -> location.getLocationCode() != null)
				.collect(Collectors.toMap(WmsLocation::getLocationCode, Function.identity(), (a, b) -> a));
		Map<Long, String> zoneNames = wmsZoneService.listByWarehouse(detail.getWarehouseId()).stream()
				.collect(Collectors.toMap(WmsZone::getId, WmsZone::getZoneName, (a, b) -> a));
		for (LocationTransferItemVO item : items) {
			WmsLocation source = locationByCode.get(item.getSourceLocationCode());
			WmsLocation target = locationByCode.get(item.getTargetLocationCode());
			item.setSourceZoneName(source == null ? null : zoneNames.get(source.getZoneId()));
			item.setTargetZoneName(target == null ? null : zoneNames.get(target.getZoneId()));
		}
		detail.setItems(items);
		detail.setPrintablePallets(LocationTransferStatus.COMPLETED.name().equals(detail.getOrderStatus())
				? printablePallets(itemService.getByOrderId(id)) : Collections.emptyList());
		int total = items.stream().mapToInt(i -> i.getQuantity() == null ? 0 : i.getQuantity()).sum();
		detail.setItemCount(items.size());
		detail.setTotalQuantity(total);
		return detail;
	}

	/**
	 * 生成调整单号：LT + yyyyMMdd + 4 位序号。
	 */
	public String generateTransferNo() {
		String prefix = "LT" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		int count = baseMapper.countTodayOrders(prefix);
		return prefix + String.format("%04d", count + 1);
	}

	// ==================== 平台：新建 / 完成 / 撤销 ====================

	/**
	 * 平台新建库位调整单：逐条校验(不动库存) → 建单(PENDING) + 明细。
	 * @param dto 新建DTO（items 每行=源批次 + 目标库位 + 数量）
	 * @return 调整单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long create(LocationTransferCreateDTO dto) {
		assertPlatform();
		Assert.notEmpty(dto.getItems(), "调整明细不能为空");
		Assert.notNull(dto.getErpTenantId(), "货主不能为空");
		Assert.notNull(dto.getWarehouseId(), "仓库不能为空");
		Assert.isTrue(LocationTransferReason.isManualReason(dto.getReasonCode()), "请选择有效的调整原因");
		if (LocationTransferReason.OTHER.name().equals(dto.getReasonCode())) {
			Assert.hasText(dto.getReason(), "选择“其他”时必须填写原因说明");
		}
		warehouseService.validateOperableOwnWarehouse(dto.getWarehouseId());
		SysTenant owner = sysTenantMapper.selectById(dto.getErpTenantId());
		Assert.notNull(owner, "货主不存在");

		List<PalletSlotVO> warehouseSlots = palletService.listSlots(dto.getWarehouseId());
		List<LocationTransferItem> items = new ArrayList<>();
		Set<Long> submittedBatchIds = new HashSet<>();
		for (LocationTransferItemDTO d : dto.getItems()) {
			Assert.notNull(d.getPhysicalInventoryId(), "源批次不能为空");
			Assert.isTrue(submittedBatchIds.add(d.getPhysicalInventoryId()), "同一源批次不能重复提交");
			WmsPhysicalInventory batch = physicalInventoryService.getById(d.getPhysicalInventoryId());
			Assert.notNull(batch, "批次不存在：" + d.getPhysicalInventoryId());
			Assert.isTrue(dto.getWarehouseId().equals(batch.getWarehouseId()), "批次不属于所选仓库");
			Assert.isTrue(dto.getErpTenantId().equals(batch.getErpTenantId()), "批次不属于所选货主");

			// 建单时校验四约束与可用量（不执行、不动库存）
			String moveMode = normalizeMoveMode(d.getMoveMode());
			boolean exactTarget = d.getTargetSlotId() != null || d.getTargetPalletId() != null;
			LocationTransferService.Resolution r;
			if (LocationTransferService.MOVE_WHOLE_PALLET.equals(moveMode)) {
				r = exactTarget
						? locationTransferService.resolveWholePalletLine(batch, dto.getWarehouseId(),
								d.getTargetLocationCode(), d.getTargetSlotId(), d.getQuantity())
						: locationTransferService.resolveWholePalletLine(batch, dto.getWarehouseId(),
								d.getTargetLocationCode(), d.getQuantity());
			}
			else {
				r = exactTarget
						? locationTransferService.resolveLine(batch, dto.getWarehouseId(),
								d.getTargetLocationCode(), d.getTargetSlotId(), d.getTargetPalletId(), d.getQuantity())
						: locationTransferService.resolveLine(batch, dto.getWarehouseId(),
								d.getTargetLocationCode(), d.getQuantity());
			}
			Assert.isTrue(!LocationTransferService.VirtualMove.INTO.equals(r.getVirtualMove())
							|| LocationTransferService.MOVE_WHOLE_PALLET.equals(moveMode),
					"移入虚拟库位必须整托移动");

			LocationTransferItem item = new LocationTransferItem();
			item.setErpTenantId(dto.getErpTenantId());
			item.setSkuCode(batch.getSkuCode());
			item.setPhysicalInventoryId(batch.getId());
			item.setSourceLocationCode(batch.getLocationCode());
			item.setSourceQuality(batch.getQuality());
			item.setMoveMode(moveMode);
			item.setSourcePalletId(batch.getPalletId());
			WmsPallet sourcePallet = batch.getPalletId() == null ? null : palletMapper.selectById(batch.getPalletId());
			item.setSourcePalletNo(sourcePallet == null ? null : sourcePallet.getPalletNo());
			item.setSourceSlotId(batch.getSlotId());
			item.setSourceSlotCode(sourcePallet == null ? null : sourcePallet.getSlotCode());
			item.setTargetLocationCode(d.getTargetLocationCode());
			PalletSlotVO target = warehouseSlots.stream()
					.filter(slot -> d.getTargetSlotId() != null
							? d.getTargetSlotId().equals(slot.getSlotId())
							: d.getTargetPalletId() != null && d.getTargetPalletId().equals(slot.getPalletId()))
					.findFirst().orElse(null);
			item.setTargetSlotId(d.getTargetPalletId() == null && target != null
					? target.getSlotId() : null);
			item.setTargetSlotCode(target == null ? null : target.getSlotCode());
			item.setTargetPalletId(LocationTransferService.MOVE_WHOLE_PALLET.equals(moveMode)
					? batch.getPalletId() : d.getTargetPalletId());
			item.setTargetPalletNo(LocationTransferService.MOVE_WHOLE_PALLET.equals(moveMode)
					? item.getSourcePalletNo() : target == null ? null : target.getPalletNo());
			item.setTargetZoneId(r.getTargetZoneId());
			item.setToGood(0);
			item.setQuantity(d.getQuantity());
			item.setRemark(d.getRemark());
			items.add(item);
		}
		validateWholePalletGroups(items);
		validatePartialLeavesSourcePallet(items);
		validateExactTargetUsage(items);
		validateCombinedPartialTargets(dto.getWarehouseId(), items);

		LocationTransferOrder order = new LocationTransferOrder();
		order.setWarehouseId(dto.getWarehouseId());
		order.setWmsTenantId(owner.getParentWmsTenantId());
		order.setErpTenantId(dto.getErpTenantId());
		order.setOrderStatus(LocationTransferStatus.PENDING.name());
		order.setSourceType("MANUAL");
		order.setReasonCode(dto.getReasonCode());
		order.setReason(dto.getReason());
		order.setRemark(dto.getRemark());
		order.setTransferNo(generateTransferNo());
		order.setCreateBy(currentUserId());
		saveOrderWithRetry(order);

		for (LocationTransferItem item : items) {
			item.setTransferOrderId(order.getId());
		}
		itemService.saveBatch(items);

		log.info("平台新建库位调整单, id={}, no={}, erpTenantId={}, items={}", order.getId(), order.getTransferNo(),
				order.getErpTenantId(), items.size());
		return order.getId();
	}

	@Transactional(rollbackFor = Exception.class)
	public List<Long> createBatch(LocationTransferBatchCreateDTO dto) {
		assertPlatform();
		Assert.notNull(dto.getWarehouseId(), "仓库不能为空");
		Assert.notEmpty(dto.getItems(), "调整明细不能为空");
		Map<Long, List<LocationTransferItemDTO>> byOwner = new java.util.LinkedHashMap<>();
		Set<Long> submitted = new HashSet<>();
		Map<Long, Long> slotOwner = new java.util.HashMap<>();
		for (LocationTransferItemDTO item : dto.getItems()) {
			Assert.notNull(item.getPhysicalInventoryId(), "源批次不能为空");
			Assert.isTrue(submitted.add(item.getPhysicalInventoryId()), "同一源批次不能重复提交");
			WmsPhysicalInventory batch = physicalInventoryService.getById(item.getPhysicalInventoryId());
			Assert.notNull(batch, "源批次不存在：" + item.getPhysicalInventoryId());
			Assert.isTrue(dto.getWarehouseId().equals(batch.getWarehouseId()), "源批次不属于所选仓库");
			byOwner.computeIfAbsent(batch.getErpTenantId(), key -> new ArrayList<>()).add(item);
			if (item.getTargetSlotId() != null) {
				Long previousOwner = slotOwner.putIfAbsent(item.getTargetSlotId(), batch.getErpTenantId());
				Assert.isTrue(previousOwner == null || previousOwner.equals(batch.getErpTenantId()),
						"不同货主不能在同一次调整中使用同一个目标托位");
			}
		}
		List<Long> ids = new ArrayList<>();
		for (Map.Entry<Long, List<LocationTransferItemDTO>> entry : byOwner.entrySet()) {
			LocationTransferCreateDTO ownerOrder = new LocationTransferCreateDTO();
			ownerOrder.setWarehouseId(dto.getWarehouseId());
			ownerOrder.setErpTenantId(entry.getKey());
			ownerOrder.setReasonCode(dto.getReasonCode());
			ownerOrder.setReason(dto.getReason());
			ownerOrder.setRemark(dto.getRemark());
			ownerOrder.setItems(entry.getValue());
			ids.add(create(ownerOrder));
		}
		return ids;
	}

	/**
	 * Reserves the virtual-location portion of an outbound order and creates a
	 * location adjustment plan that warehouse staff must complete.
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long createOutboundPlan(SalesOutboundOrder outbound, List<StockShortageVO> shortages) {
		Assert.notEmpty(shortages, "出库调整缺口不能为空");
		String sourceKey = "SALES_OUTBOUND:" + outbound.getId();
		LocationTransferOrder existing = baseMapper.selectBySourceKey(sourceKey);
		if (existing != null && !LocationTransferStatus.CANCELLED.name().equals(existing.getOrderStatus())) {
			return existing.getId();
		}

		List<LocationTransferItem> planItems = new ArrayList<>();
		for (StockShortageVO shortage : shortages) {
			int remaining = shortage.getShortage() == null ? 0 : shortage.getShortage();
			List<WmsPhysicalInventory> batches = physicalInventoryMapper.selectVirtualAllocatableForUpdate(
					outbound.getErpTenantId(), outbound.getWarehouseId(), shortage.getSkuCode());
			for (WmsPhysicalInventory batch : batches) {
				if (remaining <= 0) {
					break;
				}
				int available = Math.max((batch.getQuantity() == null ? 0 : batch.getQuantity())
						- (batch.getReservedQty() == null ? 0 : batch.getReservedQty()), 0);
				int take = Math.min(available, remaining);
				if (take <= 0) {
					continue;
				}
				physicalInventoryService.changeReservedQuantity(batch.getId(), take);

				LocationTransferItem item = new LocationTransferItem();
				item.setErpTenantId(outbound.getErpTenantId());
				item.setSkuCode(batch.getSkuCode());
				item.setPhysicalInventoryId(batch.getId());
				item.setSourceLocationCode(batch.getLocationCode());
				item.setSourceQuality(batch.getQuality());
				item.setQuantity(take);
				item.setToGood(0);
				item.setRemark("销售出库待转入物理拣货位");
				planItems.add(item);
				remaining -= take;
			}
			if (remaining > 0) {
				throw new BusinessException(409, "SKU[" + shortage.getSkuCode()
						+ "]暂存库存已被占用，请刷新后重试");
			}
		}

		LocationTransferOrder plan = existing == null ? new LocationTransferOrder() : existing;
		plan.setWarehouseId(outbound.getWarehouseId());
		SysTenant outboundOwner = sysTenantMapper.selectById(outbound.getErpTenantId());
		plan.setWmsTenantId(outboundOwner == null ? 0L : outboundOwner.getParentWmsTenantId());
		plan.setErpTenantId(outbound.getErpTenantId());
		plan.setOrderStatus(LocationTransferStatus.PLANNED.name());
		plan.setSourceType("SALES_OUTBOUND");
		plan.setSourceId(outbound.getId());
		plan.setSourceNo(outbound.getOutboundNo());
		plan.setSourceKey(sourceKey);
		plan.setReasonCode("OUTBOUND_PICKABLE_SHORTAGE");
		plan.setReason("可用库存位于暂存库位，需先调整到物理拣货位");
		plan.setTransferNo(generateTransferNo());
		plan.setCreateBy(currentUserId());
		if (existing == null) {
			saveOrderWithRetry(plan);
		}
		else {
			itemService.deleteByOrderId(plan.getId());
			plan.setCompleteTime(null);
			plan.setCompleteBy(null);
			this.updateById(plan);
		}
		for (LocationTransferItem item : planItems) {
			item.setTransferOrderId(plan.getId());
		}
		itemService.saveBatch(planItems);
		return plan.getId();
	}

	@Transactional(rollbackFor = Exception.class)
	public void completePlan(Long id, LocationTransferPlanDTO dto) {
		assertPlatform();
		LocationTransferOrder order = requireOrderForUpdate(id);
		Assert.isTrue(LocationTransferStatus.PLANNED.name().equals(order.getOrderStatus()),
				"只有待完善的库位调整计划可以完善");
		Map<Long, String> targetByItemId = dto.getItems().stream()
				.collect(Collectors.toMap(LocationTransferPlanItemDTO::getId,
						LocationTransferPlanItemDTO::getTargetLocationCode, (a, b) -> a));
		List<LocationTransferItem> items = itemService.getByOrderId(id);
		Assert.isTrue(items.size() == targetByItemId.size(), "必须为全部计划明细选择目标库位");
		for (LocationTransferItem item : items) {
			String targetCode = targetByItemId.get(item.getId());
			Assert.hasText(targetCode, "目标库位不能为空");
			WmsPhysicalInventory source = physicalInventoryService.getById(item.getPhysicalInventoryId());
			Assert.notNull(source, "源批次不存在：" + item.getPhysicalInventoryId());
			if ("SALES_OUTBOUND".equals(order.getSourceType())) {
				source.setReservedQty(Math.max((source.getReservedQty() == null ? 0 : source.getReservedQty())
						- item.getQuantity(), 0));
			}
			LocationTransferService.Resolution resolution = locationTransferService.resolveLine(source,
					order.getWarehouseId(), targetCode, item.getQuantity());
			item.setTargetLocationCode(targetCode);
			item.setTargetZoneId(resolution.getTargetZoneId());
			item.setToGood(0);
			itemService.updateById(item);
		}
		order.setOrderStatus(LocationTransferStatus.PENDING.name());
		this.updateById(order);
	}

	/**
	 * 平台执行「调整完成」（仅待调整）：逐条再次校验并执行移库 → COMPLETED。
	 */
	@Transactional(rollbackFor = Exception.class)
	public List<PalletSummaryVO> complete(Long id) {
		assertPlatform();
		LocationTransferOrder order = requireOrderForUpdate(id);
		boolean outboundPlan = "SALES_OUTBOUND".equals(order.getSourceType());
		Assert.isTrue(LocationTransferStatus.PENDING.name().equals(order.getOrderStatus()),
				"只有待调整的库位调整单可以执行");

		List<LocationTransferItem> items = itemService.getByOrderId(id);
		Assert.notEmpty(items, "调整明细为空");
		validateWholePalletGroups(items);
		validatePartialLeavesSourcePallet(items);
		validateExactTargetUsage(items);
		validateCombinedPartialTargets(order.getWarehouseId(), items);
		Map<Long, LocationTransferService.Resolution> wholePalletResolutions = new java.util.HashMap<>();
		for (LocationTransferItem item : items) {
			if (!isWholePallet(item)) {
				continue;
			}
			WmsPhysicalInventory source = physicalInventoryService.getById(item.getPhysicalInventoryId());
			Assert.notNull(source, "源批次不存在：" + item.getPhysicalInventoryId());
			Assert.isTrue(item.getSourcePalletId().equals(source.getPalletId()),
					"源批次所属托盘已变化，请取消调整单后重新创建");
			LocationTransferService.Resolution resolution = item.getTargetSlotId() == null
					? locationTransferService.resolveWholePalletLine(
							source, order.getWarehouseId(), item.getTargetLocationCode(), item.getQuantity())
					: locationTransferService.resolveWholePalletLine(
							source, order.getWarehouseId(), item.getTargetLocationCode(),
							item.getTargetSlotId(), item.getQuantity());
			wholePalletResolutions.putIfAbsent(item.getSourcePalletId(), resolution);
		}
		Set<Long> movedWholePallets = new HashSet<>();
		Map<Long, WmsPallet> createdTargetPallets = new java.util.HashMap<>();
		for (LocationTransferItem item : items) {
			if (outboundPlan) {
				physicalInventoryService.changeReservedQuantity(item.getPhysicalInventoryId(), -item.getQuantity());
			}
			WmsPhysicalInventory source = physicalInventoryService.getById(item.getPhysicalInventoryId());
			Assert.notNull(source, "源批次不存在：" + item.getPhysicalInventoryId());
			if (isWholePallet(item)) {
				if (movedWholePallets.add(item.getSourcePalletId())) {
					LocationTransferService.Resolution r = wholePalletResolutions.get(item.getSourcePalletId());
					if (LocationTransferService.VirtualMove.INTO.equals(r.getVirtualMove())) {
						physicalInventoryService.wholePalletToContainer(item.getSourcePalletId(),
								item.getTargetLocationCode(), r.getTargetZoneId(), order.getTransferNo());
					}
					else {
						physicalInventoryService.wholePalletTransfer(item.getSourcePalletId(),
								item.getTargetLocationCode(), r.getTargetZoneId(),
								item.getTargetSlotId(), r.getTargetAllocatable(), order.getTransferNo());
					}
				}
				continue;
			}
			// 执行时再次校验（不冻结，防止建单后库存变化）
			WmsPallet createdTarget = item.getTargetSlotId() == null
					? null : createdTargetPallets.get(item.getTargetSlotId());
			Long effectiveTargetSlotId = createdTarget == null && item.getTargetPalletId() == null
					? item.getTargetSlotId() : null;
			Long effectiveTargetPalletId = createdTarget == null
					? item.getTargetPalletId() : createdTarget.getId();
			boolean exactTarget = effectiveTargetSlotId != null || effectiveTargetPalletId != null;
			LocationTransferService.Resolution r = exactTarget
					? locationTransferService.resolveLine(source, order.getWarehouseId(),
							item.getTargetLocationCode(), effectiveTargetSlotId,
							effectiveTargetPalletId, item.getQuantity())
					: locationTransferService.resolveLine(source, order.getWarehouseId(),
							item.getTargetLocationCode(), item.getQuantity());
			Assert.isTrue(!LocationTransferService.VirtualMove.INTO.equals(r.getVirtualMove()),
					"移入虚拟库位必须整托移动");
			switch (r.getVirtualMove()) {
				case INTO:
					throw new IllegalStateException("移入虚拟库位必须整托移动");
				case OUTOF:
					// 从虚拟库位取回到标准区或暂存区，并按目标区域恢复库存口径。
					WmsPallet retrievedTarget = physicalInventoryService.retrieveFromContainer(
							source.getId(), item.getQuantity(),
							item.getTargetLocationCode(), r.getTargetZoneId(), effectiveTargetSlotId,
							effectiveTargetPalletId, r.getTargetAllocatable(), order.getTransferNo());
					if (item.getTargetSlotId() != null) {
						createdTargetPallets.putIfAbsent(item.getTargetSlotId(), retrievedTarget);
					}
					updateTargetPallet(item, retrievedTarget);
					break;
				default:
					WmsPallet targetPallet = exactTarget
							? physicalInventoryService.locationTransfer(source.getId(), item.getQuantity(),
									item.getTargetLocationCode(), r.getTargetZoneId(), effectiveTargetSlotId,
									effectiveTargetPalletId, r.getTargetAllocatable(), order.getTransferNo())
							: physicalInventoryService.locationTransfer(source.getId(), item.getQuantity(),
									item.getTargetLocationCode(), r.getTargetZoneId(),
									r.getTargetAllocatable(), order.getTransferNo());
					if (item.getTargetSlotId() != null) {
						createdTargetPallets.putIfAbsent(item.getTargetSlotId(), targetPallet);
					}
					updateTargetPallet(item, targetPallet);
			}
		}

		if (outboundPlan) {
			SalesOutboundOrder outbound = salesOutboundMapper.selectByIdForUpdate(order.getSourceId());
			Assert.notNull(outbound, "关联销售出库单不存在");
			Assert.isTrue(OutboundOrderStatus.WAITING_TRANSFER.name().equals(outbound.getOrderStatus()),
					"关联销售出库单已不再等待库位调整");
			List<SalesOutboundOrderItem> outboundItems = salesOutboundItemMapper
					.selectByOutboundOrderId(outbound.getId());
			List<StockShortageVO> shortages = outboundPickingService.reserveMissingForOrder(outbound, outboundItems);
			if (!shortages.isEmpty()) {
				throw new BusinessException(409, "库位调整后可拣库存仍不足：" + shortages.get(0).getSkuCode());
			}
			int updated = salesOutboundMapper.casOrderStatus(outbound.getId(),
					OutboundOrderStatus.WAITING_TRANSFER.name(), OutboundOrderStatus.CONFIRMED.name());
			Assert.isTrue(updated == 1, "销售出库单状态已变化，请刷新后重试");
		}

		order.setOrderStatus(LocationTransferStatus.COMPLETED.name());
		order.setCompleteTime(LocalDateTime.now());
		order.setCompleteBy(currentUserId());
		this.updateById(order);
		log.info("平台完成库位调整, id={}, no={}, items={}", order.getId(), order.getTransferNo(), items.size());
		return printablePallets(items);
	}

	/**
	 * 平台撤销（仅待调整）：PENDING → CANCELLED。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void cancel(Long id) {
		assertPlatform();
		LocationTransferOrder order = requireOrderForUpdate(id);
		Assert.isTrue(LocationTransferStatus.PENDING.name().equals(order.getOrderStatus())
						|| LocationTransferStatus.PLANNED.name().equals(order.getOrderStatus()),
				"只有待调整的库位调整单可以撤销");
		cancelPlan(order, true);
		log.info("平台撤销库位调整单, id={}, no={}", order.getId(), order.getTransferNo());
	}

	@Transactional(rollbackFor = Exception.class)
	public void cancelOutboundPlan(Long outboundOrderId) {
		LocationTransferOrder plan = baseMapper.selectBySourceKey("SALES_OUTBOUND:" + outboundOrderId);
		if (plan != null && (LocationTransferStatus.PLANNED.name().equals(plan.getOrderStatus())
				|| LocationTransferStatus.PENDING.name().equals(plan.getOrderStatus()))) {
			cancelPlan(plan, false);
		}
	}

	private void cancelPlan(LocationTransferOrder order, boolean restoreOutboundDraft) {
		if ("SALES_OUTBOUND".equals(order.getSourceType())) {
			for (LocationTransferItem item : itemService.getByOrderId(order.getId())) {
				physicalInventoryService.changeReservedQuantity(item.getPhysicalInventoryId(), -item.getQuantity());
			}
			SalesOutboundOrder outbound = salesOutboundMapper.selectByIdForUpdate(order.getSourceId());
			if (outbound != null) {
				outboundPickingService.releaseForOrder(outbound);
				if (restoreOutboundDraft) {
					int updated = salesOutboundMapper.casOrderStatus(outbound.getId(),
							OutboundOrderStatus.WAITING_TRANSFER.name(), OutboundOrderStatus.DRAFT.name());
					Assert.isTrue(updated == 1, "销售出库单状态已变化，请刷新后重试");
				}
			}
		}
		order.setOrderStatus(LocationTransferStatus.CANCELLED.name());
		this.updateById(order);
	}

	/**
	 * 删除（仅已取消可删；已完成需留痕不可删）。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void delete(List<Long> ids) {
		for (Long id : ids) {
			LocationTransferOrder order = requireOrder(id);
			Assert.isTrue(LocationTransferStatus.CANCELLED.name().equals(order.getOrderStatus()),
					"只有已取消的库位调整单可以删除");
			itemService.deleteByOrderId(id);
			this.removeById(id);
			log.info("删除库位调整单, id={}, no={}", order.getId(), order.getTransferNo());
		}
	}

	// ==================== 内部 ====================

	private LocationTransferOrder requireOrder(Long id) {
		LocationTransferOrder order = this.getById(id);
		Assert.notNull(order, "库位调整单不存在");
		return order;
	}

	private LocationTransferOrder requireOrderForUpdate(Long id) {
		LocationTransferOrder order = baseMapper.selectByIdForUpdate(id);
		Assert.notNull(order, "库位调整单不存在");
		return order;
	}

	private void assertPlatform() {
		String type = tenantIdentityService.currentIdentity(null).getIdentityType();
		if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(type)) {
			throw new BusinessException(403, "仅海外仓平台可操作库位调整单");
		}
	}

	private void scopeToOwnerIfErpUser(LocationTransferQO qo) {
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

	private String normalizeMoveMode(String moveMode) {
		if (moveMode == null || moveMode.trim().isEmpty()
				|| LocationTransferService.MOVE_PARTIAL.equalsIgnoreCase(moveMode)) {
			return LocationTransferService.MOVE_PARTIAL;
		}
		Assert.isTrue(LocationTransferService.MOVE_WHOLE_PALLET.equalsIgnoreCase(moveMode),
				"不支持的库位调整方式：" + moveMode);
		return LocationTransferService.MOVE_WHOLE_PALLET;
	}

	private boolean isWholePallet(LocationTransferItem item) {
		return LocationTransferService.MOVE_WHOLE_PALLET.equals(item.getMoveMode());
	}

	private void validateWholePalletGroups(List<LocationTransferItem> items) {
		items.stream().filter(this::isWholePallet)
				.forEach(item -> Assert.notNull(item.getSourcePalletId(), "整托调整明细未绑定源托盘"));
		Map<Long, List<LocationTransferItem>> wholeGroups = items.stream()
				.filter(this::isWholePallet)
				.collect(Collectors.groupingBy(LocationTransferItem::getSourcePalletId));
		for (Map.Entry<Long, List<LocationTransferItem>> entry : wholeGroups.entrySet()) {
			Long palletId = entry.getKey();
			List<WmsPhysicalInventory> current = physicalInventoryMapper.listByPalletId(palletId);
			Assert.notEmpty(current, "整托没有可移动库存：" + palletId);
			Map<Long, LocationTransferItem> submitted = entry.getValue().stream()
					.collect(Collectors.toMap(LocationTransferItem::getPhysicalInventoryId,
							Function.identity(), (a, b) -> a));
			Assert.isTrue(submitted.size() == current.size()
							&& current.stream().allMatch(batch -> submitted.containsKey(batch.getId())),
					"整托调整必须包含托盘上的全部货物");
			Set<String> targets = entry.getValue().stream()
					.map(LocationTransferItem::getTargetLocationCode).collect(Collectors.toSet());
			Assert.isTrue(targets.size() == 1, "同一整托的全部货物必须移动到同一目标库位");
			Set<Long> targetSlots = entry.getValue().stream()
					.map(LocationTransferItem::getTargetSlotId)
					.filter(java.util.Objects::nonNull).collect(Collectors.toSet());
			Assert.isTrue(targetSlots.size() <= 1, "同一整托的全部货物必须移动到同一目标托位");
			for (WmsPhysicalInventory batch : current) {
				LocationTransferItem submittedItem = submitted.get(batch.getId());
				int reserved = batch.getReservedQty() == null ? 0 : batch.getReservedQty();
				Assert.isTrue(reserved == 0, "托盘存在已预留货物，不能整托调整：" + palletId);
				Assert.isTrue(batch.getQuantity().equals(submittedItem.getQuantity()),
						"整托调整必须移动托盘上的全部数量");
			}
		}
	}

	private void validatePartialLeavesSourcePallet(List<LocationTransferItem> items) {
		Map<Long, Integer> movingByPallet = items.stream()
				.filter(item -> !isWholePallet(item) && item.getSourcePalletId() != null)
				.collect(Collectors.groupingBy(LocationTransferItem::getSourcePalletId,
						Collectors.summingInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())));
		for (Map.Entry<Long, Integer> entry : movingByPallet.entrySet()) {
			int currentQuantity = physicalInventoryMapper.listByPalletId(entry.getKey()).stream()
					.mapToInt(batch -> batch.getQuantity() == null ? 0 : batch.getQuantity())
					.sum();
			Assert.isTrue(entry.getValue() < currentQuantity,
					"拆零移动后源托盘必须保留货物；如需移动全部货物，请选择整托移动");
		}
	}

	private void validateExactTargetUsage(List<LocationTransferItem> items) {
		Map<Long, Long> wholePalletByTargetSlot = new java.util.HashMap<>();
		Set<Long> partialTargetSlots = new HashSet<>();
		for (LocationTransferItem item : items) {
			Long targetSlotId = item.getTargetSlotId();
			if (targetSlotId == null) {
				continue;
			}
			if (isWholePallet(item)) {
				Assert.isTrue(!partialTargetSlots.contains(targetSlotId),
						"同一目标托位不能同时用于整托和拆零调整");
				Long previousPalletId = wholePalletByTargetSlot.putIfAbsent(
						targetSlotId, item.getSourcePalletId());
				Assert.isTrue(previousPalletId == null || previousPalletId.equals(item.getSourcePalletId()),
						"两个不同整托不能使用同一个目标托位");
			}
			else {
				Assert.isTrue(!wholePalletByTargetSlot.containsKey(targetSlotId),
						"同一目标托位不能同时用于整托和拆零调整");
				partialTargetSlots.add(targetSlotId);
			}
		}
	}

	private void validateCombinedPartialTargets(Long warehouseId, List<LocationTransferItem> items) {
		Warehouse warehouse = warehouseService.getById(warehouseId);
		Assert.notNull(warehouse, "仓库不存在");
		int maxKinds = warehouse.getMaxSkuKindsPerPallet() == null
				? 4 : warehouse.getMaxSkuKindsPerPallet();
		Map<String, List<LocationTransferItem>> groups = items.stream()
				.filter(item -> !isWholePallet(item))
				.filter(item -> item.getTargetSlotId() != null || item.getTargetPalletId() != null)
				.collect(Collectors.groupingBy(item -> item.getTargetPalletId() != null
						? "P:" + item.getTargetPalletId() : "S:" + item.getTargetSlotId()));
		for (Map.Entry<String, List<LocationTransferItem>> entry : groups.entrySet()) {
			List<LocationTransferItem> incoming = entry.getValue();
			Set<Long> owners = incoming.stream().map(LocationTransferItem::getErpTenantId)
					.collect(Collectors.toSet());
			Set<String> qualities = incoming.stream().map(LocationTransferItem::getSourceQuality)
					.collect(Collectors.toSet());
			Set<String> kinds = incoming.stream()
					.map(item -> item.getErpTenantId() + "|" + item.getSkuCode())
					.collect(Collectors.toSet());
			Long targetPalletId = incoming.get(0).getTargetPalletId();
			if (targetPalletId != null) {
				WmsPallet pallet = palletMapper.selectById(targetPalletId);
				Assert.notNull(pallet, "目标托盘不存在：" + targetPalletId);
				Assert.isTrue(WmsPalletService.PARTIAL.equals(pallet.getPalletStatus()),
						"目标托盘不是可接收货物的半托状态：" + pallet.getPalletNo());
				List<WmsPhysicalInventory> current = physicalInventoryMapper.listByPalletId(targetPalletId);
				owners.addAll(current.stream().map(WmsPhysicalInventory::getErpTenantId)
						.collect(Collectors.toSet()));
				qualities.addAll(current.stream().map(WmsPhysicalInventory::getQuality)
						.collect(Collectors.toSet()));
				kinds.addAll(current.stream()
						.map(batch -> batch.getErpTenantId() + "|" + batch.getSkuCode())
						.collect(Collectors.toSet()));
			}
			Assert.isTrue(owners.size() <= 1, "同一目标托盘禁止跨货主混托");
			Assert.isTrue(qualities.size() <= 1, "同一目标托盘禁止混放不同品质货物");
			Assert.isTrue(kinds.size() <= maxKinds, "单托最多允许 " + maxKinds + " 种不同货物");
		}
	}

	private void updateTargetPallet(LocationTransferItem item, WmsPallet targetPallet) {
		item.setTargetPalletId(targetPallet.getId());
		item.setTargetPalletNo(targetPallet.getPalletNo());
		item.setTargetSlotId(targetPallet.getCurrentSlotId());
		item.setTargetSlotCode(targetPallet.getSlotCode());
		itemService.updateById(item);
	}

	private List<PalletSummaryVO> printablePallets(List<LocationTransferItem> items) {
		Set<Long> palletIds = new java.util.LinkedHashSet<>();
		for (LocationTransferItem item : items) {
			if (item.getSourcePalletId() != null) {
				palletIds.add(item.getSourcePalletId());
			}
			if (item.getTargetPalletId() != null) {
				palletIds.add(item.getTargetPalletId());
			}
		}
		return palletService.summaries(palletIds).stream()
				.filter(pallet -> !WmsPalletService.CLOSED.equals(pallet.getPalletStatus()))
				.filter(pallet -> pallet.getItems() != null && !pallet.getItems().isEmpty())
				.collect(Collectors.toList());
	}

	private void saveOrderWithRetry(LocationTransferOrder order) {
		for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
			try {
				this.save(order);
				return;
			}
			catch (DuplicateKeyException e) {
				if (attempt == MAX_RETRY_ATTEMPTS) {
					throw new IllegalStateException("库位调整单号生成失败，请重试", e);
				}
				order.setTransferNo(generateTransferNo());
				order.setId(null);
			}
		}
	}

}
