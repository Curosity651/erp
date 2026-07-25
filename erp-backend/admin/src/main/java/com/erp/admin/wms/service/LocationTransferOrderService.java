package com.erp.admin.wms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import com.erp.admin.wms.model.dto.LocationTransferCreateDTO;
import com.erp.admin.wms.model.dto.LocationTransferItemDTO;
import com.erp.admin.wms.model.dto.LocationTransferPlanDTO;
import com.erp.admin.wms.model.dto.LocationTransferPlanItemDTO;
import com.erp.admin.wms.model.entity.LocationTransferItem;
import com.erp.admin.wms.model.entity.LocationTransferOrder;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.enums.LocationTransferStatus;
import com.erp.admin.wms.model.enums.LocationTransferReason;
import com.erp.admin.wms.model.enums.OutboundOrderStatus;
import com.erp.admin.wms.model.qo.LocationTransferQO;
import com.erp.admin.wms.model.vo.LocationTransferDetailVO;
import com.erp.admin.wms.model.vo.LocationTransferItemVO;
import com.erp.admin.wms.model.vo.LocationTransferPageVO;
import com.erp.admin.wms.model.vo.LocationTransferStatsVO;
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
		detail.setItems(items);
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

		List<LocationTransferItem> items = new ArrayList<>();
		for (LocationTransferItemDTO d : dto.getItems()) {
			Assert.notNull(d.getPhysicalInventoryId(), "源批次不能为空");
			WmsPhysicalInventory batch = physicalInventoryService.getById(d.getPhysicalInventoryId());
			Assert.notNull(batch, "批次不存在：" + d.getPhysicalInventoryId());
			Assert.isTrue(dto.getWarehouseId().equals(batch.getWarehouseId()), "批次不属于所选仓库");
			Assert.isTrue(dto.getErpTenantId().equals(batch.getErpTenantId()), "批次不属于所选货主");

			// 建单时校验四约束与可用量（不执行、不动库存）
			LocationTransferService.Resolution r = locationTransferService.resolveLine(batch, dto.getWarehouseId(),
					d.getTargetLocationCode(), d.getQuantity());

			LocationTransferItem item = new LocationTransferItem();
			item.setErpTenantId(dto.getErpTenantId());
			item.setSkuCode(batch.getSkuCode());
			item.setPhysicalInventoryId(batch.getId());
			item.setSourceLocationCode(batch.getLocationCode());
			item.setSourceQuality(batch.getQuality());
			item.setTargetLocationCode(d.getTargetLocationCode());
			item.setTargetZoneId(r.getTargetZoneId());
			item.setToGood(r.isToGood() ? 1 : 0);
			item.setQuantity(d.getQuantity());
			item.setRemark(d.getRemark());
			items.add(item);
		}

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
		LocationTransferOrder order = requireOrder(id);
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
	public void complete(Long id) {
		assertPlatform();
		LocationTransferOrder order = requireOrder(id);
		boolean outboundPlan = "SALES_OUTBOUND".equals(order.getSourceType());
		Assert.isTrue(LocationTransferStatus.PENDING.name().equals(order.getOrderStatus()),
				"只有待调整的库位调整单可以执行");

		List<LocationTransferItem> items = itemService.getByOrderId(id);
		Assert.notEmpty(items, "调整明细为空");
		for (LocationTransferItem item : items) {
			if (outboundPlan) {
				physicalInventoryService.changeReservedQuantity(item.getPhysicalInventoryId(), -item.getQuantity());
			}
			WmsPhysicalInventory source = physicalInventoryService.getById(item.getPhysicalInventoryId());
			Assert.notNull(source, "源批次不存在：" + item.getPhysicalInventoryId());
			// 执行时再次校验（不冻结，防止建单后库存变化）
			LocationTransferService.Resolution r = locationTransferService.resolveLine(source, order.getWarehouseId(),
					item.getTargetLocationCode(), item.getQuantity());
			switch (r.getVirtualMove()) {
				case INTO:
					// 收纳积压货进虚拟库位：货主数量不变、服务商看不见、排除自动发货挑拣
					physicalInventoryService.moveToContainer(source.getId(), item.getQuantity(),
							item.getTargetLocationCode(), r.getTargetZoneId(), order.getTransferNo());
					break;
				case OUTOF:
					// 从虚拟库位取回到标准库位
					physicalInventoryService.retrieveFromContainer(source.getId(), item.getQuantity(),
							item.getTargetLocationCode(), r.getTargetZoneId(), order.getTransferNo());
					break;
				default:
					physicalInventoryService.locationTransfer(source.getId(), item.getQuantity(),
							item.getTargetLocationCode(), r.getTargetZoneId(), r.isToGood(), order.getTransferNo());
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
	}

	/**
	 * 平台撤销（仅待调整）：PENDING → CANCELLED。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void cancel(Long id) {
		assertPlatform();
		LocationTransferOrder order = requireOrder(id);
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
