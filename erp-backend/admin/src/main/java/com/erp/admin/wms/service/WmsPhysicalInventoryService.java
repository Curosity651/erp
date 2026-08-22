package com.erp.admin.wms.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.erp.admin.wms.mapper.InventoryMapper;
import com.erp.admin.wms.mapper.StockFlowMapper;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.model.dto.PutawayDTO;
import com.erp.admin.wms.model.dto.InboundPutawayDTO;
import com.erp.admin.wms.model.entity.WmsPallet;
import com.erp.admin.wms.model.vo.PalletSlotVO;
import com.erp.admin.wms.model.entity.Inventory;
import com.erp.admin.wms.model.entity.StockFlow;
import com.erp.admin.wms.model.entity.WmsLocationSlot;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 批次级库存 SSOT 服务（D1·方案②）。
 *
 * <p>
 * 唯一可写自有仓库存的入口之一（上架）。写批次 → 同事务聚合刷新 wms_inventory + 写 wms_stock_flow。 FBO
 * 同步路径（FboStockSyncController）不走这里。
 * </p>
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class WmsPhysicalInventoryService extends ExtendServiceImpl<WmsPhysicalInventoryMapper, WmsPhysicalInventory> {

	private static final String QUALITY_DAMAGED = "DAMAGED";

	private static final String QUALITY_GOOD = "GOOD";

	private final WmsInventoryAggregator aggregator;

	private final InventoryMapper inventoryMapper;

	private final StockFlowMapper stockFlowMapper;

	private final PrincipalAttributeAccessor principalAttributeAccessor;

	private final WmsPalletService palletService;

	private final SysTenantMapper sysTenantMapper;

	private final StocktakeFreezeService stocktakeFreezeService;

	/**
	 * Returns the stock that automatic outbound picking can actually allocate.
	 */
	public Map<String, Integer> getAllocatableQuantityMap(Long erpTenantId, Long warehouseId,
			Collection<String> skuCodes) {
		if (erpTenantId == null || warehouseId == null || skuCodes == null || skuCodes.isEmpty()) {
			return Collections.emptyMap();
		}
		List<WmsPhysicalInventory> batches = this.baseMapper.selectList(
				WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
						.eq(WmsPhysicalInventory::getErpTenantId, erpTenantId)
						.eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
						.in(WmsPhysicalInventory::getSkuCode, skuCodes)
						.eq(WmsPhysicalInventory::getQuality, QUALITY_GOOD)
						.eq(WmsPhysicalInventory::getAllocatable, 1)
						.ne(WmsPhysicalInventory::getContainerStored, 1));
		Map<String, Integer> result = new HashMap<>();
		for (WmsPhysicalInventory batch : batches) {
			int quantity = batch.getQuantity() == null ? 0 : batch.getQuantity();
			int reserved = batch.getReservedQty() == null ? 0 : batch.getReservedQty();
			result.merge(batch.getSkuCode(), Math.max(quantity - reserved, 0), Integer::sum);
		}
		return result;
	}

	/**
	 * Returns all owner-visible available stock, including stock in virtual
	 * locations. Virtual stock still needs a location adjustment before picking.
	 */
	public Map<String, Integer> getOwnerAvailableQuantityMap(Long erpTenantId, Long warehouseId,
			Collection<String> skuCodes) {
		if (erpTenantId == null || warehouseId == null || skuCodes == null || skuCodes.isEmpty()) {
			return Collections.emptyMap();
		}
		List<WmsPhysicalInventory> batches = this.baseMapper.selectList(
				WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
						.eq(WmsPhysicalInventory::getErpTenantId, erpTenantId)
						.eq(WmsPhysicalInventory::getWarehouseId, warehouseId)
						.in(WmsPhysicalInventory::getSkuCode, skuCodes)
						.eq(WmsPhysicalInventory::getQuality, QUALITY_GOOD)
						.eq(WmsPhysicalInventory::getAllocatable, 1));
		Map<String, Integer> result = new HashMap<>();
		for (WmsPhysicalInventory batch : batches) {
			int quantity = batch.getQuantity() == null ? 0 : batch.getQuantity();
			int reserved = batch.getReservedQty() == null ? 0 : batch.getReservedQty();
			result.merge(batch.getSkuCode(), Math.max(quantity - reserved, 0), Integer::sum);
		}
		return result;
	}

	public void changeReservedQuantity(Long batchId, int delta) {
		WmsPhysicalInventory batch = requireBatch(batchId);
		int current = safeReserved(batch);
		int next = current + delta;
		if (next < 0 || next > batch.getQuantity()) {
			throw new BusinessException(409, "批次预留数量已变化，请重试：" + batchId);
		}
		batch.setReservedQty(next);
		batch.setUpdateBy(currentUserId());
		updateBatchLocked(batch);
		aggregator.refreshSnapshot(batch.getWmsTenantId(), batch.getErpTenantId(), batch.getWarehouseId(),
				batch.getSkuCode());
	}

	/**
	 * 上架写入一个批次，并同事务聚合刷新快照 + 记一条入库流水。
	 * @param dto 上架请求
	 * @return 写入的批次
	 */
	@Transactional(rollbackFor = Exception.class)
	public WmsPhysicalInventory putaway(PutawayDTO dto) {
		stocktakeFreezeService.assertLocationMutable(dto.getWarehouseId(), dto.getLocationCode());
		Long uid = currentUserId();
		long wmsTenantId = resolveWmsTenantId(dto.getErpTenantId(), dto.getWmsTenantId());
		String quality = dto.getQuality() == null ? QUALITY_GOOD : dto.getQuality();
		int allocatable = dto.getAllocatable() == null ? 1 : dto.getAllocatable();
		LocalDate inboundDate = dto.getInboundDate() == null ? LocalDate.now(ZoneOffset.UTC) : dto.getInboundDate();
		long inboundItemId = dto.getInboundItemId() == null ? 0L : dto.getInboundItemId();

		WmsPallet autoPallet = null;
		if (dto.getPalletId() == null && dto.getLocationCode() != null) {
			PalletSlotVO slot = palletService.listSlots(dto.getWarehouseId()).stream()
					.filter(value -> dto.getLocationCode().equals(value.getLocationCode()))
					.filter(value -> WmsPalletService.EMPTY.equals(value.getSlotStatus()))
					.sorted(java.util.Comparator.comparing(PalletSlotVO::getLevelNo))
					.findFirst().orElseThrow(() -> new BusinessException(409,
							"库位三个托盘层位均已占用：" + dto.getLocationCode()));
			InboundPutawayDTO.PutawayLine line = new InboundPutawayDTO.PutawayLine();
			line.setSkuCode(dto.getSkuCode());
			line.setQuantity(dto.getQuantity());
			line.setQuality(quality);
			line.setSlotCode(slot.getSlotCode());
			line.setPalletKey("AUTO-" + java.util.UUID.randomUUID());
			line.setCapacitySource("MANUAL_REQUIRED");
			autoPallet = palletService.createForPutaway(dto.getWarehouseId(), dto.getErpTenantId(),
					slot.getSlotCode(), java.util.Collections.singletonList(line));
			dto.setPalletId(autoPallet.getId());
			dto.setSlotId(autoPallet.getCurrentSlotId());
		}

		// 同日 FIFO 次序
		int pickOrder = physicalInventoryMapper().countSameDay(wmsTenantId, dto.getErpTenantId(), dto.getWarehouseId(),
				dto.getSkuCode(), inboundDate) + 1;

		// 刷新前的快照值（用于流水 before/after）
		boolean damaged = QUALITY_DAMAGED.equals(quality);
		Inventory prior = inventoryMapper.selectByTenantKey(wmsTenantId, dto.getErpTenantId(), dto.getWarehouseId(),
				dto.getSkuCode());
		int beforeVal = prior == null ? 0 : bucketValue(prior, damaged);

		WmsPhysicalInventory batch = new WmsPhysicalInventory();
		batch.setWmsTenantId(wmsTenantId);
		batch.setErpTenantId(dto.getErpTenantId());
		batch.setWarehouseId(dto.getWarehouseId());
		batch.setSkuCode(dto.getSkuCode());
		batch.setInboundItemId(inboundItemId);
		batch.setInboundDate(inboundDate);
		batch.setPickOrder(pickOrder);
		batch.setQuantity(dto.getQuantity());
		batch.setReservedQty(0);
		batch.setQuality(quality);
		batch.setLocationCode(dto.getLocationCode());
		batch.setPalletId(dto.getPalletId());
		batch.setSlotId(dto.getSlotId());
		batch.setZoneId(dto.getZoneId());
		batch.setAllocatable(allocatable);
		batch.setCreateBy(uid);
		batch.setUpdateBy(uid);
		this.baseMapper.insert(batch);
		if (autoPallet != null) {
			palletService.refreshAfterInventoryChange(autoPallet.getId());
		}

		// 同事务聚合刷新快照
		WmsInventoryAggregator.Buckets buckets = aggregator.refreshSnapshot(wmsTenantId, dto.getErpTenantId(),
				dto.getWarehouseId(), dto.getSkuCode());

		// 记一条入库流水
		int afterVal = damaged ? buckets.damaged : buckets.available;
		writeFlow(dto, wmsTenantId, damaged, beforeVal, afterVal, uid);

		return batch;
	}

	/**
	 * 冻结一个批次的部分数量（报废发起：占住 reserved_qty，可用随之下降），同事务刷新快照。
	 * <p>要求 available = quantity - reserved_qty ≥ qty，否则报错。
	 * @param batchId 批次ID
	 * @param qty     冻结数量（>0）
	 * @return 冻结后的批次
	 */
	@Transactional(rollbackFor = Exception.class)
	public WmsPhysicalInventory reserveBatch(Long batchId, int qty) {
		WmsPhysicalInventory batch = requireBatch(batchId);
		int available = batch.getQuantity() - safeReserved(batch);
		if (qty <= 0 || qty > available) {
			throw new BusinessException(400, "冻结数量非法：批次[" + batchId + "]可用" + available + "，请求" + qty);
		}
		batch.setReservedQty(safeReserved(batch) + qty);
		updateBatchLocked(batch);
		aggregator.refreshSnapshot(batch.getWmsTenantId(), batch.getErpTenantId(), batch.getWarehouseId(),
				batch.getSkuCode());
		return batch;
	}

	/**
	 * 释放一个批次此前冻结的数量（报废驳回/撤销），同事务刷新快照。
	 * @param batchId 批次ID
	 * @param qty     释放数量（>0）
	 */
	@Transactional(rollbackFor = Exception.class)
	public void releaseBatch(Long batchId, int qty) {
		WmsPhysicalInventory batch = requireBatch(batchId);
		int release = Math.min(Math.max(qty, 0), safeReserved(batch));
		batch.setReservedQty(safeReserved(batch) - release);
		updateBatchLocked(batch);
		aggregator.refreshSnapshot(batch.getWmsTenantId(), batch.getErpTenantId(), batch.getWarehouseId(),
				batch.getSkuCode());
	}

	/**
	 * 报废扣减一个批次（货主确认销毁）：quantity 与 reserved_qty 同时扣 qty，刷快照并写 SCRAP 出库流水。
	 * @param batchId  批次ID
	 * @param qty      报废数量（>0，且此前已冻结）
	 * @param sourceNo 来源单号（报废单号）
	 */
	@Transactional(rollbackFor = Exception.class)
	public void scrapBatch(Long batchId, int qty, String sourceNo) {
		WmsPhysicalInventory batch = requireBatch(batchId);
		if (qty <= 0 || qty > batch.getQuantity()) {
			throw new BusinessException(400, "报废数量非法：批次[" + batchId + "]现存" + batch.getQuantity() + "，请求" + qty);
		}
		boolean damaged = QUALITY_DAMAGED.equals(batch.getQuality());
		Inventory prior = inventoryMapper.selectByTenantKey(batch.getWmsTenantId(), batch.getErpTenantId(),
				batch.getWarehouseId(), batch.getSkuCode());
		int before = prior == null ? 0 : bucketValue(prior, damaged);

		batch.setQuantity(batch.getQuantity() - qty);
		batch.setReservedQty(Math.max(safeReserved(batch) - qty, 0));
		updateBatchLocked(batch);

		WmsInventoryAggregator.Buckets buckets = aggregator.refreshSnapshot(batch.getWmsTenantId(),
				batch.getErpTenantId(), batch.getWarehouseId(), batch.getSkuCode());
		int after = damaged ? buckets.damaged : buckets.available;
		writeScrapFlow(batch, qty, before, after, sourceNo);
		palletService.refreshAfterInventoryChange(batch.getPalletId());
	}

	/**
	 * 库内移库：把源批次的部分数量移到目标库位（同仓）。源批扣减、目标库位合并或新建批次，
	 * 同事务刷新快照并写 LOCATION_TRANSFER 流水。品质/分区的合法性由 LocationTransferService 校验后传入。
	 * @param sourceBatchId      源批次ID
	 * @param qty                移动数量（>0，且不超过源批可用）
	 * @param targetLocationCode 目标库位编码
	 * @param targetZoneId       目标库位分区ID
	 * @param targetAllocatable  目标分区是否参与可出库分配
	 * @param sourceNo           流水来源单号（可空）
	 */
	@Transactional(rollbackFor = Exception.class)
	public WmsPallet locationTransfer(Long sourceBatchId, int qty, String targetLocationCode, Long targetZoneId,
			int targetAllocatable, String sourceNo) {
		return locationTransfer(sourceBatchId, qty, targetLocationCode, targetZoneId,
				null, null, targetAllocatable, sourceNo);
	}

	@Transactional(rollbackFor = Exception.class)
	public WmsPallet locationTransfer(Long sourceBatchId, int qty, String targetLocationCode, Long targetZoneId,
			Long targetSlotId, Long targetPalletId, int targetAllocatable, String sourceNo) {
		WmsPhysicalInventory source = requireBatch(sourceBatchId);
		stocktakeFreezeService.assertLocationMutable(source.getWarehouseId(), targetLocationCode);
		int available = source.getQuantity() - safeReserved(source);
		if (qty <= 0 || qty > available) {
			throw new BusinessException(400, "移库数量非法：源批次[" + sourceBatchId + "]可用" + available + "，请求" + qty);
		}
		String targetQuality = source.getQuality();
		WmsPallet targetPallet = targetSlotId == null && targetPalletId == null
				? palletService.allocateForTransfer(source.getWarehouseId(), targetLocationCode,
						source.getErpTenantId(), source.getSkuCode(), targetQuality, qty)
				: palletService.allocateForTransfer(source.getWarehouseId(), targetLocationCode,
						targetSlotId, targetPalletId, source.getErpTenantId(), source.getSkuCode(), targetQuality, qty);

		// 源批扣减
		source.setQuantity(source.getQuantity() - qty);
		updateBatchLocked(source);

		// 目标库位：同 SKU + 同入库批次 + 同品质 → 合并，否则新建
		WmsPhysicalInventory target = this.baseMapper.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
				.eq(WmsPhysicalInventory::getWmsTenantId, source.getWmsTenantId())
				.eq(WmsPhysicalInventory::getErpTenantId, source.getErpTenantId())
				.eq(WmsPhysicalInventory::getWarehouseId, source.getWarehouseId())
				.eq(WmsPhysicalInventory::getSkuCode, source.getSkuCode())
				.eq(WmsPhysicalInventory::getLocationCode, targetLocationCode)
				.eq(WmsPhysicalInventory::getPalletId, targetPallet.getId())
				.eq(WmsPhysicalInventory::getInboundItemId, source.getInboundItemId())
				.eq(WmsPhysicalInventory::getQuality, targetQuality))
				.stream().findFirst().orElse(null);

		Long uid = currentUserId();
		if (target != null) {
			target.setQuantity(target.getQuantity() + qty);
			target.setZoneId(targetZoneId);
			target.setAllocatable(targetAllocatable);
			target.setUpdateBy(uid);
			updateBatchLocked(target);
		}
		else {
			WmsPhysicalInventory nb = new WmsPhysicalInventory();
			nb.setWmsTenantId(source.getWmsTenantId());
			nb.setErpTenantId(source.getErpTenantId());
			nb.setWarehouseId(source.getWarehouseId());
			nb.setSkuCode(source.getSkuCode());
			nb.setInboundItemId(source.getInboundItemId());
			nb.setInboundDate(source.getInboundDate());
			nb.setPickOrder(physicalInventoryMapper().countSameDay(source.getWmsTenantId(), source.getErpTenantId(),
					source.getWarehouseId(), source.getSkuCode(), source.getInboundDate()) + 1);
			nb.setQuantity(qty);
			nb.setReservedQty(0);
			nb.setQuality(targetQuality);
			nb.setLocationCode(targetLocationCode);
			nb.setPalletId(targetPallet.getId());
			nb.setSlotId(targetPallet.getCurrentSlotId());
			nb.setZoneId(targetZoneId);
			nb.setAllocatable(targetAllocatable);
			nb.setContainerStored(0);
			nb.setCreateBy(uid);
			nb.setUpdateBy(uid);
			this.baseMapper.insert(nb);
		}

		aggregator.refreshSnapshot(source.getWmsTenantId(), source.getErpTenantId(), source.getWarehouseId(),
				source.getSkuCode());
		palletService.refreshAfterInventoryChange(source.getPalletId());
		palletService.refreshAfterInventoryChange(targetPallet.getId());
		writeTransferFlow(source, source.getLocationCode(), qty, targetLocationCode, targetQuality, sourceNo, uid);
		return targetPallet;
	}

	@Transactional(rollbackFor = Exception.class)
	public void wholePalletTransfer(Long palletId, String targetLocationCode, Long targetZoneId,
			int targetAllocatable, String sourceNo) {
		wholePalletTransfer(palletId, targetLocationCode, targetZoneId, null, targetAllocatable, sourceNo);
	}

	@Transactional(rollbackFor = Exception.class)
	public void wholePalletTransfer(Long palletId, String targetLocationCode, Long targetZoneId,
			Long targetSlotId, int targetAllocatable, String sourceNo) {
		List<WmsPhysicalInventory> batches = this.baseMapper.selectByPalletIdForUpdate(palletId);
		Assert.notEmpty(batches, "源托盘没有可移动库存");
		stocktakeFreezeService.assertLocationMutable(batches.get(0).getWarehouseId(),
				batches.get(0).getLocationCode());
		stocktakeFreezeService.assertLocationMutable(batches.get(0).getWarehouseId(), targetLocationCode);
		Assert.isTrue(batches.stream().allMatch(batch -> safeReserved(batch) == 0),
				"整托存在已预留库存，不能整托调整");
		String sourceLocationCode = batches.get(0).getLocationCode();
		boolean fromVirtual = batches.stream().allMatch(this::containerStored);
		Assert.isTrue(fromVirtual || batches.stream().noneMatch(this::containerStored),
				"源托盘同时存在物理和虚拟库存，不能整托调整");
		WmsLocationSlot targetSlot;
		if (fromVirtual) {
			targetSlot = targetSlotId == null
					? palletService.moveWholePalletFromVirtual(palletId, targetLocationCode)
					: palletService.moveWholePalletFromVirtual(palletId, targetLocationCode, targetSlotId);
		}
		else {
			targetSlot = targetSlotId == null
					? palletService.moveWholePallet(palletId, targetLocationCode)
					: palletService.moveWholePallet(palletId, targetLocationCode, targetSlotId);
		}
		Long uid = currentUserId();
		for (WmsPhysicalInventory batch : batches) {
			Assert.isTrue(sourceLocationCode.equals(batch.getLocationCode()), "源托盘库存位置不一致，不能整托调整");
			batch.setLocationCode(targetLocationCode);
			batch.setSlotId(targetSlot.getId());
			batch.setZoneId(targetZoneId);
			batch.setAllocatable(targetAllocatable);
			batch.setContainerStored(0);
			batch.setOriginLocationCode(null);
			batch.setUpdateBy(uid);
			updateBatchLocked(batch);
			writeTransferFlow(batch, sourceLocationCode, batch.getQuantity(), targetLocationCode,
					batch.getQuality(), sourceNo, uid);
			aggregator.refreshSnapshot(batch.getWmsTenantId(), batch.getErpTenantId(),
					batch.getWarehouseId(), batch.getSkuCode());
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void wholePalletToContainer(Long palletId, String targetLocationCode, Long targetZoneId,
			String sourceNo) {
		List<WmsPhysicalInventory> batches = this.baseMapper.selectByPalletIdForUpdate(palletId);
		Assert.notEmpty(batches, "源托盘没有可移动库存");
		stocktakeFreezeService.assertLocationMutable(batches.get(0).getWarehouseId(),
				batches.get(0).getLocationCode());
		stocktakeFreezeService.assertLocationMutable(batches.get(0).getWarehouseId(), targetLocationCode);
		Assert.isTrue(batches.stream().allMatch(batch -> safeReserved(batch) == 0),
				"整托存在已预留库存，不能移入虚拟库位");
		String sourceLocationCode = batches.get(0).getLocationCode();
		palletService.moveWholePalletToVirtual(palletId, targetLocationCode);
		Long uid = currentUserId();
		for (WmsPhysicalInventory batch : batches) {
			Assert.isTrue(sourceLocationCode.equals(batch.getLocationCode()),
					"源托盘库存位置不一致，不能整托调整");
			String originLocation = containerStored(batch) && batch.getOriginLocationCode() != null
					? batch.getOriginLocationCode() : batch.getLocationCode();
			batch.setLocationCode(targetLocationCode);
			batch.setSlotId(null);
			batch.setZoneId(targetZoneId);
			batch.setAllocatable(1);
			batch.setContainerStored(1);
			batch.setOriginLocationCode(originLocation);
			batch.setUpdateBy(uid);
			updateBatchLocked(batch);
			writeContainerFlow(batch, batch.getQuantity(), sourceLocationCode, targetLocationCode,
					"CONTAINER_IN", "整托移入虚拟库位 " + sourceLocationCode + "→" + targetLocationCode,
					sourceNo, uid);
			aggregator.refreshSnapshot(batch.getWmsTenantId(), batch.getErpTenantId(),
					batch.getWarehouseId(), batch.getSkuCode());
		}
	}

	private void writeTransferFlow(WmsPhysicalInventory source, String sourceLocationCode, int qty, String targetLocationCode,
			String targetQuality, String sourceNo, Long uid) {
		StockFlow flow = new StockFlow();
		flow.setWarehouseId(source.getWarehouseId());
		flow.setWmsTenantId(source.getWmsTenantId());
		flow.setErpTenantId(source.getErpTenantId());
		flow.setRegionId(0L);
		flow.setSkuCode(source.getSkuCode());
		flow.setBucket(QUALITY_DAMAGED.equals(targetQuality) ? "DAMAGED" : "AVAILABLE");
		flow.setDirection("MOVE");
		flow.setQuantity(qty);
		flow.setBeforeQuantity(0);
		flow.setAfterQuantity(0);
		flow.setPostingId(0L);
		flow.setPostingNo("");
		flow.setPostingItemId(0L);
		flow.setPostingType("LOCATION_TRANSFER");
		flow.setSourceType("LOCATION_TRANSFER");
		flow.setSourceId(0L);
		flow.setSourceNo(sourceNo == null ? "" : sourceNo);
		flow.setRemark("库内移库 " + sourceLocationCode + "→" + targetLocationCode);
		flow.setCreateBy(uid);
		stockFlowMapper.insert(flow);
	}

	/**
	 * 集装箱入箱：把源批次的部分/全部数量搬进集装箱隐藏库位（同仓）。源批扣减、集装箱库位合并/新建批次，
	 * 置 {@code container_stored=1}、记原库位；{@code allocatable} 保持 1、{@code quality} 保持 GOOD（口径甲：货主可用不变），
	 * 同事务刷新快照并写 CONTAINER_IN 流水。仅平台调用（身份校验在 ContainerStorageService）。
	 * @param sourceBatchId        源批次ID
	 * @param qty                  入箱数量（>0 且不超过源批可用 qty-reserved）
	 * @param containerLocationCode 集装箱目标库位编码
	 * @param containerZoneId       集装箱分区ID
	 * @param sourceNo             流水来源单号
	 */
	@Transactional(rollbackFor = Exception.class)
	public void moveToContainer(Long sourceBatchId, int qty, String containerLocationCode, Long containerZoneId,
			String sourceNo) {
		WmsPhysicalInventory source = requireBatch(sourceBatchId);
		stocktakeFreezeService.assertLocationMutable(source.getWarehouseId(), containerLocationCode);
		int available = source.getQuantity() - safeReserved(source);
		if (qty <= 0 || qty > available) {
			throw new BusinessException(400, "入箱数量非法：源批次[" + sourceBatchId + "]可用" + available + "，请求" + qty);
		}
		boolean alreadyVirtual = containerStored(source);
		String originLocation = alreadyVirtual && source.getOriginLocationCode() != null
				? source.getOriginLocationCode() : source.getLocationCode();
		Long uid = currentUserId();

		// 源批扣减
		source.setQuantity(source.getQuantity() - qty);
		source.setUpdateBy(uid);
		updateBatchLocked(source);

		// 集装箱库位：同 SKU + 同入库批次 + 已在箱 → 合并，否则新建（allocatable=1、quality=GOOD、container_stored=1）
		WmsPhysicalInventory target = this.baseMapper.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
				.eq(WmsPhysicalInventory::getWmsTenantId, source.getWmsTenantId())
				.eq(WmsPhysicalInventory::getErpTenantId, source.getErpTenantId())
				.eq(WmsPhysicalInventory::getWarehouseId, source.getWarehouseId())
				.eq(WmsPhysicalInventory::getSkuCode, source.getSkuCode())
				.eq(WmsPhysicalInventory::getLocationCode, containerLocationCode)
				.eq(WmsPhysicalInventory::getInboundItemId, source.getInboundItemId())
				.eq(WmsPhysicalInventory::getQuality, source.getQuality())
				.eq(WmsPhysicalInventory::getContainerStored, 1))
				.stream().findFirst().orElse(null);

		if (target != null) {
			target.setQuantity(target.getQuantity() + qty);
			target.setAllocatable(1);
			target.setUpdateBy(uid);
			updateBatchLocked(target);
		}
		else {
			WmsPhysicalInventory nb = new WmsPhysicalInventory();
			nb.setWmsTenantId(source.getWmsTenantId());
			nb.setErpTenantId(source.getErpTenantId());
			nb.setWarehouseId(source.getWarehouseId());
			nb.setSkuCode(source.getSkuCode());
			nb.setInboundItemId(source.getInboundItemId());
			nb.setInboundDate(source.getInboundDate());
			nb.setPickOrder(physicalInventoryMapper().countSameDay(source.getWmsTenantId(), source.getErpTenantId(),
					source.getWarehouseId(), source.getSkuCode(), source.getInboundDate()) + 1);
			nb.setQuantity(qty);
			nb.setReservedQty(0);
			nb.setQuality(source.getQuality());
			nb.setLocationCode(containerLocationCode);
			nb.setZoneId(containerZoneId);
			nb.setAllocatable(1);
			nb.setContainerStored(1);
			nb.setOriginLocationCode(originLocation);
			nb.setCreateBy(uid);
			nb.setUpdateBy(uid);
			this.baseMapper.insert(nb);
		}

		aggregator.refreshSnapshot(source.getWmsTenantId(), source.getErpTenantId(), source.getWarehouseId(),
				source.getSkuCode());
		palletService.refreshAfterInventoryChange(source.getPalletId());
		writeContainerFlow(source, qty, source.getLocationCode(), containerLocationCode,
				alreadyVirtual ? "CONTAINER_MOVE" : "CONTAINER_IN",
				"移入虚拟库位 " + source.getLocationCode() + "→" + containerLocationCode, sourceNo, uid);
	}

	/**
	 * 集装箱取回（调拨回）：把集装箱批次的部分/全部数量搬回正常库位。源批(集装箱)扣减、目标库位合并/新建正常批次，
	 * 置 {@code container_stored=0}、清原库位，恢复参与 FIFO 挑拣。同事务刷新快照并写 CONTAINER_OUT 流水。
	 * @param sourceBatchId      集装箱源批次ID
	 * @param qty                取回数量
	 * @param targetLocationCode 回落目标库位编码（默认原库位）
	 * @param targetZoneId       目标分区ID
	 * @param targetAllocatable  目标分区是否参与可出库分配
	 * @param sourceNo           流水来源单号
	 */
	@Transactional(rollbackFor = Exception.class)
	public WmsPallet retrieveFromContainer(Long sourceBatchId, int qty, String targetLocationCode, Long targetZoneId,
			Long targetSlotId, Long targetPalletId, int targetAllocatable, String sourceNo) {
		WmsPhysicalInventory source = requireBatch(sourceBatchId);
		stocktakeFreezeService.assertLocationMutable(source.getWarehouseId(), targetLocationCode);
		if (!containerStored(source)) {
			throw new BusinessException(400, "该批次不在集装箱中，无需取回：" + sourceBatchId);
		}
		int available = source.getQuantity() - safeReserved(source);
		if (qty <= 0 || qty > available) {
			throw new BusinessException(400, "取回数量非法：集装箱批次[" + sourceBatchId + "]可用" + available + "，请求" + qty);
		}
		Long uid = currentUserId();
		WmsPallet targetPallet = targetSlotId == null && targetPalletId == null
				? palletService.allocateForTransfer(source.getWarehouseId(), targetLocationCode,
						source.getErpTenantId(), source.getSkuCode(), source.getQuality(), qty)
				: palletService.allocateForTransfer(source.getWarehouseId(), targetLocationCode,
						targetSlotId, targetPalletId, source.getErpTenantId(), source.getSkuCode(),
						source.getQuality(), qty);

		source.setQuantity(source.getQuantity() - qty);
		source.setUpdateBy(uid);
		updateBatchLocked(source);

		// 目标正常库位：同 SKU + 同入库批次 + 未在箱 → 合并，否则新建（container_stored=0、allocatable=1）
		WmsPhysicalInventory target = this.baseMapper.selectList(WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
				.eq(WmsPhysicalInventory::getWmsTenantId, source.getWmsTenantId())
				.eq(WmsPhysicalInventory::getErpTenantId, source.getErpTenantId())
				.eq(WmsPhysicalInventory::getWarehouseId, source.getWarehouseId())
				.eq(WmsPhysicalInventory::getSkuCode, source.getSkuCode())
				.eq(WmsPhysicalInventory::getLocationCode, targetLocationCode)
				.eq(WmsPhysicalInventory::getPalletId, targetPallet.getId())
				.eq(WmsPhysicalInventory::getInboundItemId, source.getInboundItemId())
				.eq(WmsPhysicalInventory::getQuality, source.getQuality())
				.eq(WmsPhysicalInventory::getContainerStored, 0))
				.stream().findFirst().orElse(null);

		if (target != null) {
			target.setQuantity(target.getQuantity() + qty);
			target.setZoneId(targetZoneId);
			target.setAllocatable(targetAllocatable);
			target.setUpdateBy(uid);
			updateBatchLocked(target);
		}
		else {
			WmsPhysicalInventory nb = new WmsPhysicalInventory();
			nb.setWmsTenantId(source.getWmsTenantId());
			nb.setErpTenantId(source.getErpTenantId());
			nb.setWarehouseId(source.getWarehouseId());
			nb.setSkuCode(source.getSkuCode());
			nb.setInboundItemId(source.getInboundItemId());
			nb.setInboundDate(source.getInboundDate());
			nb.setPickOrder(physicalInventoryMapper().countSameDay(source.getWmsTenantId(), source.getErpTenantId(),
					source.getWarehouseId(), source.getSkuCode(), source.getInboundDate()) + 1);
			nb.setQuantity(qty);
			nb.setReservedQty(0);
			nb.setQuality(source.getQuality());
			nb.setLocationCode(targetLocationCode);
			nb.setPalletId(targetPallet.getId());
			nb.setSlotId(targetPallet.getCurrentSlotId());
			nb.setZoneId(targetZoneId);
			nb.setAllocatable(targetAllocatable);
			nb.setContainerStored(0);
			nb.setCreateBy(uid);
			nb.setUpdateBy(uid);
			this.baseMapper.insert(nb);
		}

		aggregator.refreshSnapshot(source.getWmsTenantId(), source.getErpTenantId(), source.getWarehouseId(),
				source.getSkuCode());
		palletService.refreshAfterInventoryChange(source.getPalletId());
		palletService.refreshAfterInventoryChange(targetPallet.getId());
		writeContainerFlow(source, qty, source.getLocationCode(), targetLocationCode, "CONTAINER_OUT",
				"集装箱取回 " + source.getLocationCode() + "→" + targetLocationCode, sourceNo, uid);
		return targetPallet;
	}

	long resolveWmsTenantId(Long erpTenantId, Long requestedWmsTenantId) {
		Assert.notNull(erpTenantId, "货主不能为空");
		SysTenant owner = sysTenantMapper.selectById(erpTenantId);
		Assert.notNull(owner, "货主不存在：" + erpTenantId);
		Long expected = owner.getParentWmsTenantId();
		Assert.notNull(expected, "货主未关联 WMS 服务商：" + erpTenantId);
		if (requestedWmsTenantId != null && requestedWmsTenantId > 0) {
			Assert.isTrue(expected.equals(requestedWmsTenantId), "上架库存的服务商归属与货主不一致");
		}
		return expected;
	}

	private boolean containerStored(WmsPhysicalInventory batch) {
		return batch.getContainerStored() != null && batch.getContainerStored() == 1;
	}

	/**
	 * 跨仓调拨发出：扣减源仓(A)源批次的部分数量（从可用扣），同事务刷新快照 + 写 TRANSFER_SHIP 出库流水。
	 * 可用量随之下降；在途桶由调拨编排层另行过账维护（与本方法不重叠）。
	 * @param batchId  源批次ID
	 * @param qty      发出数量（>0 且不超过源批可用 qty-reserved）
	 * @param sourceNo 调拨单号
	 */
	@Transactional(rollbackFor = Exception.class)
	public void deductForTransferShip(Long batchId, int qty, String sourceNo) {
		WmsPhysicalInventory batch = requireBatch(batchId);
		int available = batch.getQuantity() - safeReserved(batch);
		if (qty <= 0 || qty > available) {
			throw new BusinessException(400, "调拨发出数量非法：批次[" + batchId + "]可用" + available + "，请求" + qty);
		}
		boolean damaged = QUALITY_DAMAGED.equals(batch.getQuality());
		Inventory prior = inventoryMapper.selectByTenantKey(batch.getWmsTenantId(), batch.getErpTenantId(),
				batch.getWarehouseId(), batch.getSkuCode());
		int before = prior == null ? 0 : bucketValue(prior, damaged);

		batch.setQuantity(batch.getQuantity() - qty);
		batch.setUpdateBy(currentUserId());
		updateBatchLocked(batch);

		WmsInventoryAggregator.Buckets buckets = aggregator.refreshSnapshot(batch.getWmsTenantId(),
				batch.getErpTenantId(), batch.getWarehouseId(), batch.getSkuCode());
		int after = damaged ? buckets.damaged : buckets.available;

		StockFlow flow = new StockFlow();
		flow.setWarehouseId(batch.getWarehouseId());
		flow.setWmsTenantId(batch.getWmsTenantId());
		flow.setErpTenantId(batch.getErpTenantId());
		flow.setRegionId(0L);
		flow.setSkuCode(batch.getSkuCode());
		flow.setBucket(damaged ? "DAMAGED" : "AVAILABLE");
		flow.setDirection("OUT");
		flow.setQuantity(qty);
		flow.setBeforeQuantity(before);
		flow.setAfterQuantity(after);
		flow.setPostingId(0L);
		flow.setPostingNo("");
		flow.setPostingItemId(0L);
		flow.setPostingType("TRANSFER_SHIP");
		flow.setSourceType("TRANSFER");
		flow.setSourceId(0L);
		flow.setSourceNo(sourceNo == null ? "" : sourceNo);
		flow.setRemark("跨仓调拨发出 " + batch.getLocationCode());
		flow.setCreateBy(currentUserId());
		stockFlowMapper.insert(flow);
	}

	/**
	 * 撤回恢复：把在途调拨发出扣掉的数量加回原源批次（与 {@link #deductForTransferShip} 对称）。
	 * <p>直接对原批次行加量 + 同事务刷新快照 + 写一条 TRANSFER_CANCEL 入库流水；
	 * 不新建批次，避免 putaway 盲插撞 uk_batch(…, sku_code, inbound_item_id, location_code) 唯一键。
	 * @param batchId  源批次ID（= 调拨明细的 sourcePhysicalInventoryId）
	 * @param qty      恢复数量（&gt;0）
	 * @param sourceNo 调拨单号（写入流水来源）
	 */
	@Transactional(rollbackFor = Exception.class)
	public void restoreForTransferRevoke(Long batchId, int qty, String sourceNo) {
		if (qty <= 0) {
			throw new BusinessException(400, "撤回恢复数量非法：" + qty);
		}
		WmsPhysicalInventory batch = requireBatch(batchId);
		boolean damaged = QUALITY_DAMAGED.equals(batch.getQuality());
		Inventory prior = inventoryMapper.selectByTenantKey(batch.getWmsTenantId(), batch.getErpTenantId(),
				batch.getWarehouseId(), batch.getSkuCode());
		int before = prior == null ? 0 : bucketValue(prior, damaged);

		batch.setQuantity(batch.getQuantity() + qty);
		batch.setUpdateBy(currentUserId());
		updateBatchLocked(batch);

		WmsInventoryAggregator.Buckets buckets = aggregator.refreshSnapshot(batch.getWmsTenantId(),
				batch.getErpTenantId(), batch.getWarehouseId(), batch.getSkuCode());
		int after = damaged ? buckets.damaged : buckets.available;

		StockFlow flow = new StockFlow();
		flow.setWarehouseId(batch.getWarehouseId());
		flow.setWmsTenantId(batch.getWmsTenantId());
		flow.setErpTenantId(batch.getErpTenantId());
		flow.setRegionId(0L);
		flow.setSkuCode(batch.getSkuCode());
		flow.setBucket(damaged ? "DAMAGED" : "AVAILABLE");
		flow.setDirection("IN");
		flow.setQuantity(qty);
		flow.setBeforeQuantity(before);
		flow.setAfterQuantity(after);
		flow.setPostingId(0L);
		flow.setPostingNo("");
		flow.setPostingItemId(0L);
		flow.setPostingType("TRANSFER_CANCEL");
		flow.setSourceType("TRANSFER");
		flow.setSourceId(0L);
		flow.setSourceNo(sourceNo == null ? "" : sourceNo);
		flow.setRemark("跨仓调拨撤回恢复 " + batch.getLocationCode());
		flow.setCreateBy(currentUserId());
		stockFlowMapper.insert(flow);
	}

	private void writeContainerFlow(WmsPhysicalInventory source, int qty, String fromLoc, String toLoc,
			String postingType, String remark, String sourceNo, Long uid) {
		StockFlow flow = new StockFlow();
		flow.setWarehouseId(source.getWarehouseId());
		flow.setWmsTenantId(source.getWmsTenantId());
		flow.setErpTenantId(source.getErpTenantId());
		flow.setRegionId(0L);
		flow.setSkuCode(source.getSkuCode());
		flow.setBucket("AVAILABLE");
		flow.setDirection("MOVE");
		flow.setQuantity(qty);
		flow.setBeforeQuantity(0);
		flow.setAfterQuantity(0);
		flow.setPostingId(0L);
		flow.setPostingNo("");
		flow.setPostingItemId(0L);
		flow.setPostingType(postingType);
		flow.setSourceType("CONTAINER_STORAGE");
		flow.setSourceId(0L);
		flow.setSourceNo(sourceNo == null ? "" : sourceNo);
		flow.setRemark(remark);
		flow.setCreateBy(uid);
		stockFlowMapper.insert(flow);
	}

	private WmsPhysicalInventory requireBatch(Long batchId) {
		WmsPhysicalInventory batch = this.baseMapper.selectByIdForUpdate(batchId);
		if (batch == null) {
			throw new BusinessException(404, "批次不存在：" + batchId);
		}
		stocktakeFreezeService.assertLocationMutable(batch.getWarehouseId(), batch.getLocationCode());
		return batch;
	}

	private int safeReserved(WmsPhysicalInventory batch) {
		return batch.getReservedQty() == null ? 0 : batch.getReservedQty();
	}

	/** 乐观锁更新批次，冲突即报错（与 ship 口径一致）。 */
	private void updateBatchLocked(WmsPhysicalInventory batch) {
		int updated = this.baseMapper.updateById(batch);
		if (updated != 1) {
			throw new BusinessException(409, "批次库存版本冲突，请重试：" + batch.getId());
		}
	}

	private void writeScrapFlow(WmsPhysicalInventory batch, int qty, int before, int after, String sourceNo) {
		StockFlow flow = new StockFlow();
		flow.setWarehouseId(batch.getWarehouseId());
		flow.setWmsTenantId(batch.getWmsTenantId());
		flow.setErpTenantId(batch.getErpTenantId());
		flow.setRegionId(0L);
		flow.setSkuCode(batch.getSkuCode());
		flow.setBucket(QUALITY_DAMAGED.equals(batch.getQuality()) ? "DAMAGED" : "AVAILABLE");
		flow.setDirection("OUT");
		flow.setQuantity(qty);
		flow.setBeforeQuantity(before);
		flow.setAfterQuantity(after);
		flow.setPostingId(0L);
		flow.setPostingNo("");
		flow.setPostingItemId(0L);
		flow.setPostingType("SCRAP");
		flow.setSourceType("ADJUSTMENT");
		flow.setSourceId(0L);
		flow.setSourceNo(sourceNo == null ? "" : sourceNo);
		flow.setRemark("报废销毁(货主确认)");
		flow.setCreateBy(currentUserId());
		stockFlowMapper.insert(flow);
	}

	/**
	 * 按货主只读查询批次明细（C4：ERP/OMS 只读，按 erp_tenant_id 过滤）。
	 */
	public List<WmsPhysicalInventory> listByErpTenant(Long erpTenantId, Long warehouseId, String skuKeyword) {
		return this.baseMapper.listByErpTenant(erpTenantId, warehouseId, skuKeyword);
	}

	/**
	 * 集装箱入箱候选批次（良品+可分配+未入箱+有余量）。
	 */
	public List<WmsPhysicalInventory> listContainerCandidates(Long warehouseId, Long erpTenantId, String skuKeyword) {
		return this.baseMapper.listContainerCandidates(warehouseId, erpTenantId, skuKeyword);
	}

	/**
	 * 当前已入集装箱的批次（平台追踪货去哪了）。
	 */
	public List<WmsPhysicalInventory> listContainerStored(Long warehouseId, Long erpTenantId, String skuKeyword) {
		return this.baseMapper.listContainerStored(warehouseId, erpTenantId, skuKeyword);
	}

	/**
	 * 某仓库已占用库位编码集合（存在批次即占用；上架库位独占校验用）。
	 */
	public List<String> occupiedLocationCodes(Long warehouseId) {
		return this.baseMapper.listBlockingPhysicalLocationCodes(warehouseId);
	}

	/**
	 * 某仓库指定库位上现存的批次（移库目标库位占用/可合并校验用）。
	 */
	public List<WmsPhysicalInventory> listAtLocation(Long warehouseId, String locationCode) {
		return this.baseMapper.listByWarehouseAndLocationCodes(warehouseId,
				java.util.Collections.singletonList(locationCode));
	}

	/**
	 * 按聚合键读取桶快照（演示/对账用）。
	 */
	public Inventory snapshot(Long wmsTenantId, Long erpTenantId, Long warehouseId, String skuCode) {
		long wt = wmsTenantId == null ? 0L : wmsTenantId;
		return inventoryMapper.selectByTenantKey(wt, erpTenantId, warehouseId, skuCode);
	}

	private void writeFlow(PutawayDTO dto, long wmsTenantId, boolean damaged, int before, int after, Long uid) {
		StockFlow flow = new StockFlow();
		flow.setWarehouseId(dto.getWarehouseId());
		flow.setWmsTenantId(wmsTenantId);
		flow.setErpTenantId(dto.getErpTenantId());
		flow.setRegionId(0L);
		flow.setSkuCode(dto.getSkuCode());
		flow.setBucket(damaged ? "DAMAGED" : "AVAILABLE");
		flow.setDirection("IN");
		flow.setQuantity(dto.getQuantity());
		flow.setBeforeQuantity(before);
		flow.setAfterQuantity(after);
		flow.setPostingId(0L);
		flow.setPostingNo("");
		flow.setPostingItemId(0L);
		flow.setPostingType("PUTAWAY");
		flow.setSourceType("PUTAWAY");
		flow.setSourceId(0L);
		flow.setSourceNo("");
		flow.setRemark("批次上架(方案②)");
		flow.setCreateBy(uid);
		stockFlowMapper.insert(flow);
	}

	private int bucketValue(Inventory inv, boolean damaged) {
		if (damaged) {
			return inv.getDamagedQuantity() == null ? 0 : inv.getDamagedQuantity();
		}
		return inv.getAvailableQuantity() == null ? 0 : inv.getAvailableQuantity();
	}

	private WmsPhysicalInventoryMapper physicalInventoryMapper() {
		return this.baseMapper;
	}

	private Long currentUserId() {
		try {
			return principalAttributeAccessor.getUserId();
		}
		catch (Exception ignore) {
			return null;
		}
	}

}
