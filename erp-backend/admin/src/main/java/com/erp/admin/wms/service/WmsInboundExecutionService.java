package com.erp.admin.wms.service;

import com.erp.admin.platform.finance.service.WarehouseBillingService;
import com.erp.admin.system.model.vo.SysFileVO;
import com.erp.admin.system.service.SysFileService;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.enums.WmsResultCode;
import com.erp.admin.wms.mapper.PurchaseInboundItemMapper;
import com.erp.admin.wms.mapper.PurchaseInboundMapper;
import com.erp.admin.wms.mapper.WmsPutawayReceiptLineMapper;
import com.erp.admin.wms.model.dto.InboundPutawayDTO;
import com.erp.admin.wms.model.dto.InboundReceiveDTO;
import com.erp.admin.wms.model.dto.PutawayDTO;
import com.erp.admin.wms.model.dto.StockPostingDTO;
import com.erp.admin.wms.model.dto.StockPostingItemDTO;
import com.erp.admin.wms.model.entity.PurchaseInboundOrder;
import com.erp.admin.wms.model.entity.PurchaseInboundOrderItem;
import com.erp.admin.wms.model.entity.WmsPutawayReceiptLine;
import com.erp.admin.wms.model.entity.ShippingOrder;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.enums.PostingType;
import com.erp.admin.wms.model.enums.PurchaseInboundStatus;
import com.erp.admin.wms.model.enums.SourceType;
import com.erp.admin.wms.model.enums.StockBucket;
import com.erp.admin.wms.model.enums.StockDirection;
import com.erp.admin.wms.model.vo.AvailableLocationVO;
import com.erp.admin.wms.model.vo.InboundPutawayPlanVO;
import com.erp.admin.wms.model.vo.PalletSlotVO;
import com.erp.admin.wms.model.vo.PalletSummaryVO;
import com.erp.admin.wms.model.vo.PutawayReceiptLineVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台收货 / 上架执行服务（D2，方案②落地，同一张入库单状态流转）。
 *
 * <p>货主在采购/自定义入库单页建单并「提交」(DRAFT→SUBMITTED)后，由平台超管两步作业：
 * <ul>
 * <li><b>收货</b> {@link #receive}：校验 SUBMITTED；录实收数量；有物流单则区域在途出账；SUBMITTED→RECEIVED。
 * <li><b>上架</b> {@link #putaway}：把收货数量分配到库位 → {@link WmsPhysicalInventoryService#putaway} 写批次
 * → 聚合刷新 {@code wms_inventory.available} + 流水；RECEIVED→COMPLETED。
 * </ul>
 *
 * <p>仓库可用库存唯一真源 = 上架批次聚合（旧一键 confirm 的仓库 AVAILABLE 过账已退役）。货物归属取入库单
 * {@code erp_tenant_id}（建单时按当前货主写入），用作批次的 {@code erpTenantId}。
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WmsInboundExecutionService {

	private final PurchaseInboundMapper purchaseInboundMapper;

	private final PurchaseInboundItemMapper purchaseInboundItemMapper;

	private final StockPostingService stockPostingService;

	private final ShippingOrderService shippingOrderService;

	private final ShippingOrderItemService shippingOrderItemService;

	private final PurchaseOrderService purchaseOrderService;

	private final PurchaseOrderItemService purchaseOrderItemService;

	private final WmsPhysicalInventoryService physicalInventoryService;

	private final WmsLocationService wmsLocationService;

	private final WmsZoneService wmsZoneService;

	private final SysTenantMapper sysTenantMapper;

	private final WmsRackAssignmentService wmsRackAssignmentService;

	private final TenantIdentityService tenantIdentityService;

	private final InboundPalletPlanningService inboundPalletPlanningService;

	private final WmsPalletService palletService;

	private final WarehouseBillingService warehouseBillingService;

	private final PrincipalAttributeAccessor principalAttributeAccessor;

	private final WmsPutawayReceiptLineMapper putawayReceiptLineMapper;

	private final SysFileService sysFileService;

	// ==================== 收货 ====================

	@Transactional(rollbackFor = Exception.class)
	public void receive(InboundReceiveDTO dto) {
		assertPlatform();
		PurchaseInboundOrder order = purchaseInboundMapper.selectByIdForUpdate(dto.getInboundOrderId());
		Assert.notNull(order, "入库单不存在");
		Assert.isTrue(PurchaseInboundStatus.SUBMITTED.name().equals(order.getOrderStatus()), "只有已提交的入库单可以收货");
		List<Long> evidenceFileIds = dto.getEvidenceFileIds();
		Assert.notEmpty(evidenceFileIds, "请至少上传一张收货现场照片");
		List<SysFileVO> evidenceFiles = sysFileService.getFileInfoList(evidenceFileIds);
		Assert.isTrue(evidenceFiles.size() == new HashSet<>(evidenceFileIds).size(), "收货照片不存在或已被删除");
		Assert.isTrue(evidenceFiles.stream().allMatch(file -> file.getContentType() != null
				&& file.getContentType().toLowerCase().startsWith("image/")), "收货凭证只能上传图片");

		List<PurchaseInboundOrderItem> items = purchaseInboundItemMapper.selectByInboundOrderId(order.getId());
		Map<Long, Integer> receivedByItemId = mapReceivedByItemId(items, dto.getItems());
		List<PurchaseInboundOrderItem> validItems = new ArrayList<>();
		for (PurchaseInboundOrderItem item : items) {
			int actual = receivedByItemId.getOrDefault(item.getId(), 0);
			// M-6：实收不得超过应收（收货链此前不校验，自定义链无区域在途兜底 → 超量经上架全额流入 available）。
			// 仅在建单已声明应收(expectedQuantity 非空)时约束，不误伤无应收基准的单。
			if (item.getExpectedQuantity() != null) {
				Assert.isTrue(actual <= item.getExpectedQuantity(),
						String.format("SKU[%s]实收数量(%d)不能超过应收数量(%d)",
								item.getSkuCode(), actual, item.getExpectedQuantity()));
			}
			item.setActualQuantity(actual);
			purchaseInboundItemMapper.updateById(item);
			if (actual > 0) {
				validItems.add(item);
			}
		}
		Assert.notEmpty(validItems, "至少需要一条明细的实收数量大于0");

		// 有物流单（采购链）：区域在途出账 + 物流单/采购单收货回写；自定义链(MANUAL/CUSTOM_RETURN)无在途与采购关联，跳过。
		if (order.getShippingOrderId() != null) {
			postRegionInTransitOut(order, validItems);
			// 物流单明细已到货数量（乐观校验）→ 物流单到货状态
			shippingOrderItemService.settleInboundReservations(items);
			shippingOrderService.recalculateArrivalStatus(order.getShippingOrderId());
			// 采购单明细已入库数量 → 采购单入库状态
			purchaseOrderItemService.increaseReceivedQuantity(validItems);
			updatePurchaseOrderReceivingStatus(validItems);
		}

		order.setOrderStatus(PurchaseInboundStatus.RECEIVED.name());
		order.setReceiveBy(principalAttributeAccessor.getUserId());
		order.setReceiveTime(LocalDateTime.now());
		order.setReceiveEvidenceFileIds(joinEvidenceFileIds(evidenceFileIds));
		purchaseInboundMapper.updateById(order);
		log.info("收货完成, inboundOrderId={}, skuCount={}", order.getId(), validItems.size());
	}

	public static String joinEvidenceFileIds(List<Long> fileIds) {
		return fileIds.stream().distinct().map(String::valueOf).collect(Collectors.joining(","));
	}

	public PurchaseInboundOrder findSubmittedByInboundNo(String inboundNo) {
		assertPlatform();
		Assert.hasText(inboundNo, "Inbound number is required");
		PurchaseInboundOrder order = purchaseInboundMapper.selectSubmittedByInboundNo(inboundNo.trim());
		Assert.notNull(order, "No submitted inbound order found for this number");
		return order;
	}

	public static Map<Long, Integer> mapReceivedByItemId(List<PurchaseInboundOrderItem> orderItems,
			List<InboundReceiveDTO.ReceiveItem> receivedItems) {
		Map<Long, PurchaseInboundOrderItem> itemById = orderItems.stream()
				.collect(Collectors.toMap(PurchaseInboundOrderItem::getId, item -> item));
		Map<Long, Integer> result = new LinkedHashMap<>();
		for (InboundReceiveDTO.ReceiveItem received : receivedItems) {
			PurchaseInboundOrderItem item = itemById.get(received.getInboundOrderItemId());
			Assert.notNull(item, "Inbound item does not belong to this order");
			Assert.isTrue(item.getSkuCode().equalsIgnoreCase(received.getSkuCode()),
					"Inbound item SKU does not match");
			Assert.isTrue(!result.containsKey(item.getId()), "Inbound item was submitted more than once");
			result.put(item.getId(), received.getActualQuantity());
		}
		return result;
	}

	private void postRegionInTransitOut(PurchaseInboundOrder order, List<PurchaseInboundOrderItem> validItems) {
		ShippingOrder shippingOrder = shippingOrderService.getByIdOrThrow(order.getShippingOrderId());
		Long regionId = shippingOrder.getTargetRegionId();
		// 货主维取物流单的货主：本笔区域在途 OUT 必须冲减发货时（物流单）建立的同一条
		// (货主,区域,SKU) 区域库存记录，否则会错位到别的货主或新建出多余记录。
		Long erpTenantId = shippingOrder.getErpTenantId();
		List<StockPostingItemDTO> postingItems = validItems.stream()
				.map(item -> StockPostingItemDTO.builder()
						.regionId(regionId)
						.erpTenantId(erpTenantId)
						.skuCode(item.getSkuCode())
						.bucket(StockBucket.IN_TRANSIT)
						.direction(StockDirection.OUT)
						.quantity(item.getActualQuantity())
						.build())
				.collect(Collectors.toList());
		StockPostingDTO postingDTO = StockPostingDTO.builder()
				.postingType(PostingType.PURCHASE_RECEIVE)
				.sourceType(SourceType.PURCHASE_INBOUND.name())
				.sourceId(order.getId())
				.sourceNo(order.getInboundNo())
				.warehouseId(order.getWarehouseId())
				.regionId(regionId)
				.erpTenantId(erpTenantId)
				.items(postingItems)
				.build();
		stockPostingService.post(postingDTO);
	}

	private void updatePurchaseOrderReceivingStatus(List<PurchaseInboundOrderItem> items) {
		Set<Long> purchaseOrderIds = items.stream()
				.map(PurchaseInboundOrderItem::getPurchaseOrderId)
				.filter(poId -> poId != null && poId > 0)
				.collect(Collectors.toSet());
		for (Long purchaseOrderId : purchaseOrderIds) {
			purchaseOrderService.updateReceivingStatus(purchaseOrderId);
		}
	}

	// ==================== 上架 ====================

	/**
	 * Pallet-aware putaway. The legacy quantity and inventory write path remains intact,
	 * while a three-level slot and pallet are atomically claimed before batch creation.
	 */
	@Transactional(rollbackFor = Exception.class)
	public List<PalletSummaryVO> putaway(InboundPutawayDTO dto) {
		assertPlatform();
		PurchaseInboundOrder order = purchaseInboundMapper.selectById(dto.getInboundOrderId());
		Assert.notNull(order, "入库单不存在");
		Assert.isTrue(PurchaseInboundStatus.RECEIVED.name().equals(order.getOrderStatus()),
				"只有已收货的入库单可以上架");

		int claimed = purchaseInboundMapper.casOrderStatus(order.getId(),
				PurchaseInboundStatus.RECEIVED.name(), PurchaseInboundStatus.COMPLETED.name());
		if (claimed != 1) {
			throw new BusinessException(409, "该入库单已上架或正在处理，请勿重复操作");
		}

		List<PurchaseInboundOrderItem> items = purchaseInboundItemMapper.selectByInboundOrderId(order.getId());
		Map<String, Integer> receivedBySku = items.stream()
				.filter(item -> item.getActualQuantity() != null && item.getActualQuantity() > 0)
				.collect(Collectors.toMap(PurchaseInboundOrderItem::getSkuCode,
						PurchaseInboundOrderItem::getActualQuantity, Integer::sum));
		Map<String, Integer> submittedBySku = dto.getLines().stream()
				.collect(Collectors.toMap(InboundPutawayDTO.PutawayLine::getSkuCode,
						InboundPutawayDTO.PutawayLine::getQuantity, Integer::sum));
		if (!receivedBySku.equals(submittedBySku)) {
			Set<String> skuCodes = new java.util.TreeSet<>();
			skuCodes.addAll(receivedBySku.keySet());
			skuCodes.addAll(submittedBySku.keySet());
			String differences = skuCodes.stream()
					.filter(sku -> !receivedBySku.getOrDefault(sku, 0)
							.equals(submittedBySku.getOrDefault(sku, 0)))
					.map(sku -> sku + "：实收" + receivedBySku.getOrDefault(sku, 0)
							+ "，上架" + submittedBySku.getOrDefault(sku, 0))
					.collect(Collectors.joining("；"));
			throw new BusinessException(WmsResultCode.PUTAWAY_QUANTITY_MISMATCH.getCode(),
					WmsResultCode.PUTAWAY_QUANTITY_MISMATCH.getMessage() + "（" + differences + "）");
		}
		Map<String, List<PurchaseInboundOrderItem>> receivedItemsBySku = items.stream()
				.filter(item -> item.getActualQuantity() != null && item.getActualQuantity() > 0)
				.sorted(Comparator.comparing(PurchaseInboundOrderItem::getId))
				.collect(Collectors.groupingBy(PurchaseInboundOrderItem::getSkuCode,
						LinkedHashMap::new, Collectors.toList()));
		Map<Long, Integer> remainingByItem = items.stream()
				.collect(Collectors.toMap(PurchaseInboundOrderItem::getId,
						item -> item.getActualQuantity() == null ? 0 : item.getActualQuantity()));
		Long erpTenantId = order.getErpTenantId();
		Assert.notNull(erpTenantId, "入库单未设置货主，不能上架");
		Set<String> allowedRacks = resolveAllowedRacks(order);
		Map<String, PalletSlotVO> slotByCode = palletService.listSlots(order.getWarehouseId()).stream()
				.collect(Collectors.toMap(PalletSlotVO::getSlotCode, value -> value, (a, b) -> a));

		Map<Long, Set<String>> selectionKeysByPallet = dto.getLines().stream()
				.filter(line -> line.getPalletId() != null)
				.peek(line -> Assert.hasText(line.getPalletKey(), "现有托盘必须携带托盘栏标识"))
				.collect(Collectors.groupingBy(InboundPutawayDTO.PutawayLine::getPalletId,
						Collectors.mapping(InboundPutawayDTO.PutawayLine::getPalletKey, Collectors.toSet())));
		selectionKeysByPallet.forEach((palletId, selectionKeys) ->
				Assert.isTrue(selectionKeys.size() == 1,
						"同一现有托盘不能由多个独立托盘栏重复选择：" + palletId
								+ "；如需混托，请先合并为一个托盘栏"));

		Map<String, List<InboundPutawayDTO.PutawayLine>> groups = dto.getLines().stream()
				.collect(Collectors.groupingBy(this::palletGroupKey, LinkedHashMap::new, Collectors.toList()));
		Map<String, com.erp.admin.wms.model.entity.WmsPallet> assigned = new LinkedHashMap<>();
		Set<String> newSlots = new HashSet<>();
		for (Map.Entry<String, List<InboundPutawayDTO.PutawayLine>> entry : groups.entrySet()) {
			List<InboundPutawayDTO.PutawayLine> lines = entry.getValue();
			InboundPutawayDTO.PutawayLine first = lines.get(0);
			Assert.isTrue(lines.stream().allMatch(line -> first.getSlotCode().equals(line.getSlotCode())),
					"同一托盘的所有货物必须放在同一层位");
			PalletSlotVO slot = slotByCode.get(first.getSlotCode());
			Assert.notNull(slot, "层位不存在：" + first.getSlotCode());
			Assert.isTrue(allowedRacks.contains(slot.getRackNo()),
					"该层位所在货架未分配给本货主服务商：" + first.getSlotCode());
			String quality = normalizeQuality(first.getQuality());
			Assert.isTrue(lines.stream().allMatch(line -> quality.equals(normalizeQuality(line.getQuality()))),
					"同一托盘不能混放不同品质状态");
			Assert.isTrue(zoneTypeForQuality(quality).equals(slot.getZoneType()),
					"层位分区与货物品质不匹配：" + first.getSlotCode());

			com.erp.admin.wms.model.entity.WmsPallet pallet;
			if (first.getPalletId() != null) {
				Assert.isTrue(first.getPalletId().equals(slot.getPalletId()),
						"托盘已经不在所选层位，请刷新上架计划");
				pallet = palletService.lockExistingForPutaway(first.getPalletId(), erpTenantId, lines);
			}
			else {
				Assert.isTrue(newSlots.add(first.getSlotCode()),
						"同一层位不能创建两个托盘：" + first.getSlotCode());
				pallet = palletService.createForPutaway(order.getWarehouseId(), erpTenantId,
						first.getSlotCode(), lines);
			}
			assigned.put(entry.getKey(), pallet);
		}

		for (InboundPutawayDTO.PutawayLine line : dto.getLines()) {
			PalletSlotVO slot = slotByCode.get(line.getSlotCode());
			com.erp.admin.wms.model.entity.WmsPallet pallet = assigned.get(palletGroupKey(line));
			int remaining = line.getQuantity();
			for (PurchaseInboundOrderItem item : receivedItemsBySku
					.getOrDefault(line.getSkuCode(), Collections.emptyList())) {
				int itemRemaining = remainingByItem.getOrDefault(item.getId(), 0);
				if (itemRemaining <= 0 || remaining <= 0) {
					continue;
				}
				int allocated = Math.min(itemRemaining, remaining);
				PutawayDTO put = new PutawayDTO();
				put.setWmsTenantId(pallet.getWmsTenantId());
				put.setErpTenantId(erpTenantId);
				put.setWarehouseId(order.getWarehouseId());
				put.setSkuCode(line.getSkuCode());
				put.setInboundItemId(item.getId());
				put.setQuantity(allocated);
				put.setQuality(normalizeQuality(line.getQuality()));
				put.setLocationCode(slot.getLocationCode());
				put.setPalletId(pallet.getId());
				put.setSlotId(slot.getSlotId());
				put.setZoneId(slot.getZoneId());
				put.setAllocatable(GOOD.equals(put.getQuality()) ? 1 : 0);
				physicalInventoryService.putaway(put);
				remainingByItem.put(item.getId(), itemRemaining - allocated);
				remaining -= allocated;
			}
			Assert.isTrue(remaining == 0, "SKU[" + line.getSkuCode() + "]上架明细无法匹配实收批次");
		}
		savePutawayReceiptLines(order.getId(), dto.getLines(), assigned);
		assigned.values().forEach(pallet -> palletService.refreshAfterInventoryChange(pallet.getId()));
		warehouseBillingService.recordInbound(order, items, dto.getConfirmedVolumeCbm(),
				dto.getAfterHours(), dto.getAfterHoursReason());
		int operatorRecorded = purchaseInboundMapper.recordPutawayOperator(order.getId(),
				principalAttributeAccessor.getUserId(), LocalDateTime.now());
		Assert.isTrue(operatorRecorded == 1, "上架操作员记录失败");
		log.info("Pallet putaway completed, inboundOrderId={}, palletCount={}", order.getId(), assigned.size());
		return palletService.summaries(assigned.values().stream()
				.map(com.erp.admin.wms.model.entity.WmsPallet::getId).collect(Collectors.toSet()));
	}

	public List<PalletSummaryVO> getPutawayPallets(Long inboundOrderId) {
		assertPlatform();
		PurchaseInboundOrder order = purchaseInboundMapper.selectById(inboundOrderId);
		Assert.notNull(order, "入库单不存在");
		Assert.isTrue(PurchaseInboundStatus.COMPLETED.name().equals(order.getOrderStatus()),
				"只有已完成上架的入库单可以查看托盘");
		return palletService.listByInboundOrder(inboundOrderId);
	}

	public List<PutawayReceiptLineVO> getPutawayReceiptLines(Long inboundOrderId) {
		assertPlatform();
		PurchaseInboundOrder order = purchaseInboundMapper.selectById(inboundOrderId);
		Assert.notNull(order, "入库单不存在");
		Assert.isTrue(PurchaseInboundStatus.COMPLETED.name().equals(order.getOrderStatus()),
				"只有已完成上架的入库单可以打印上架单");
		List<PutawayReceiptLineVO> snapshot = putawayReceiptLineMapper.selectByInboundOrderId(inboundOrderId);
		return snapshot.isEmpty() ? palletService.listReceiptLinesByInboundOrder(inboundOrderId) : snapshot;
	}

	private void savePutawayReceiptLines(Long inboundOrderId,
			List<InboundPutawayDTO.PutawayLine> lines,
			Map<String, com.erp.admin.wms.model.entity.WmsPallet> assigned) {
		Map<String, WmsPutawayReceiptLine> snapshots = new LinkedHashMap<>();
		for (InboundPutawayDTO.PutawayLine line : lines) {
			com.erp.admin.wms.model.entity.WmsPallet pallet = assigned.get(palletGroupKey(line));
			Assert.notNull(pallet, "上架托盘不存在");
			String quality = normalizeQuality(line.getQuality());
			String key = pallet.getId() + "|" + line.getSkuCode() + "|" + quality;
			WmsPutawayReceiptLine snapshot = snapshots.computeIfAbsent(key, ignored -> {
				WmsPutawayReceiptLine value = new WmsPutawayReceiptLine();
				value.setInboundOrderId(inboundOrderId);
				value.setPalletId(pallet.getId());
				value.setPalletNo(pallet.getPalletNo());
				value.setSlotCode(pallet.getSlotCode());
				value.setSkuCode(line.getSkuCode());
				value.setQuality(quality);
				value.setQuantity(0);
				value.setCreateTime(LocalDateTime.now());
				return value;
			});
			snapshot.setQuantity(snapshot.getQuantity() + line.getQuantity());
		}
		snapshots.values().forEach(putawayReceiptLineMapper::insert);
	}

	public InboundPutawayPlanVO planPutaway(Long inboundOrderId) {
		assertPlatform();
		PurchaseInboundOrder order = purchaseInboundMapper.selectById(inboundOrderId);
		Assert.notNull(order, "入库单不存在");
		Assert.isTrue(PurchaseInboundStatus.RECEIVED.name().equals(order.getOrderStatus()),
				"只有已收货的入库单可以生成上架计划");
		return inboundPalletPlanningService.plan(order,
				purchaseInboundItemMapper.selectByInboundOrderId(order.getId()), resolveAllowedRacks(order));
	}

	private String palletGroupKey(InboundPutawayDTO.PutawayLine line) {
		if (line.getPalletId() != null) {
			return "EXISTING-" + line.getPalletId();
		}
		Assert.hasText(line.getPalletKey(), "新托盘分组标识不能为空");
		return line.getPalletKey();
	}

	@Transactional(rollbackFor = Exception.class)
	private void putawayLegacy(InboundPutawayDTO dto) {
		assertPlatform();
		PurchaseInboundOrder order = purchaseInboundMapper.selectById(dto.getInboundOrderId());
		Assert.notNull(order, "入库单不存在");
		Assert.isTrue(PurchaseInboundStatus.RECEIVED.name().equals(order.getOrderStatus()), "只有已收货的入库单可以上架");

		// 状态 CAS 抢占：RECEIVED→COMPLETED 原子推进，并发/重试上架只有一个赢家（消灭 TOCTOU，防重复批次翻倍）。
		// 落败者在此即被挡下、不执行后续 putaway；本事务后续任何异常回滚也会一并撤销该状态推进。
		int claimed = purchaseInboundMapper.casOrderStatus(order.getId(),
				PurchaseInboundStatus.RECEIVED.name(), PurchaseInboundStatus.COMPLETED.name());
		if (claimed != 1) {
			throw new BusinessException(400, "该入库单已上架或正在处理，请勿重复操作");
		}

		List<PurchaseInboundOrderItem> items = purchaseInboundItemMapper.selectByInboundOrderId(order.getId());
		Map<String, Integer> receivedBySku = items.stream()
				.filter(i -> i.getActualQuantity() != null && i.getActualQuantity() > 0)
				.collect(Collectors.toMap(PurchaseInboundOrderItem::getSkuCode,
						PurchaseInboundOrderItem::getActualQuantity, Integer::sum));
		Map<String, Long> itemIdBySku = items.stream().collect(Collectors.toMap(
				PurchaseInboundOrderItem::getSkuCode, PurchaseInboundOrderItem::getId, (a, b) -> a));

		List<MergedLine> merged = mergeLines(dto.getLines());
		Map<String, Integer> putawayBySku = sumBySku(merged);
		if (!coversExactly(receivedBySku, putawayBySku)) {
			throw new BusinessException(WmsResultCode.PUTAWAY_QUANTITY_MISMATCH.getCode(),
					WmsResultCode.PUTAWAY_QUANTITY_MISMATCH.getMessage());
		}

		// 库位校验：真实存在于本仓 + 货架归属(本货主服务商租的货架) + 独占 + 品质↔分区强制联动
		Map<String, WmsLocation> locByCode = wmsLocationService.listByWarehouse(order.getWarehouseId()).stream()
				.filter(l -> l.getLocationCode() != null)
				.collect(Collectors.toMap(WmsLocation::getLocationCode, l -> l, (a, b) -> a));
		Map<Long, String> zoneTypeById = wmsZoneService.listByWarehouse(order.getWarehouseId()).stream()
				.collect(Collectors.toMap(WmsZone::getId, z -> z.getZoneType() == null ? "" : z.getZoneType(),
						(a, b) -> a));
		Set<String> occupied = new HashSet<>(physicalInventoryService.occupiedLocationCodes(order.getWarehouseId()));
		// 完全严格：只能上到「货主的父服务商」在本仓当前有效租用的货架排上
		Set<String> allowedRacks = resolveAllowedRacks(order);
		Set<String> usedInThisSubmit = new HashSet<>();
		for (MergedLine line : merged) {
			WmsLocation loc = locByCode.get(line.locationCode);
			if (loc == null) {
				throw new BusinessException(WmsResultCode.PUTAWAY_LOCATION_NOT_FOUND.getCode(),
						WmsResultCode.PUTAWAY_LOCATION_NOT_FOUND.getMessage() + "：" + line.locationCode);
			}
			// 货架归属：库位所在排必须是本货主服务商当前有效租用的货架
			if (!allowedRacks.contains(loc.getRackNo())) {
				throw new BusinessException(WmsResultCode.PUTAWAY_RACK_NOT_OWNED.getCode(),
						WmsResultCode.PUTAWAY_RACK_NOT_OWNED.getMessage() + "：" + line.locationCode);
			}
			// 独占：已有批次占用、或同一次提交里被多个 SKU/行占用，均拒绝
			if (occupied.contains(line.locationCode) || !usedInThisSubmit.add(line.locationCode)) {
				throw new BusinessException(WmsResultCode.PUTAWAY_LOCATION_OCCUPIED.getCode(),
						WmsResultCode.PUTAWAY_LOCATION_OCCUPIED.getMessage() + "：" + line.locationCode);
			}
			String wantZoneType = zoneTypeForQuality(line.quality);
			String actualZoneType = loc.getZoneId() == null ? null : zoneTypeById.get(loc.getZoneId());
			if (!wantZoneType.equals(actualZoneType)) {
				throw new BusinessException(WmsResultCode.PUTAWAY_ZONE_QUALITY_MISMATCH.getCode(),
						WmsResultCode.PUTAWAY_ZONE_QUALITY_MISMATCH.getMessage() + "：" + line.locationCode);
			}
		}

		Long erpTenantId = order.getErpTenantId() == null ? 1L : order.getErpTenantId();
		for (MergedLine line : merged) {
			PutawayDTO put = new PutawayDTO();
			put.setWmsTenantId(0L);
			put.setErpTenantId(erpTenantId);
			put.setWarehouseId(order.getWarehouseId());
			put.setSkuCode(line.skuCode);
			put.setInboundItemId(itemIdBySku.getOrDefault(line.skuCode, 0L));
			put.setQuantity(line.quantity);
			put.setQuality(line.quality);
			put.setLocationCode(line.locationCode);
			// zoneId 以库位实际分区为准（不信任前端传值）
			put.setZoneId(locByCode.get(line.locationCode).getZoneId());
			put.setAllocatable(GOOD.equals(line.quality) ? 1 : 0);
			physicalInventoryService.putaway(put);
		}

		// 状态已在方法开头 CAS 抢占为 COMPLETED，此处不再重复无条件写。
		log.info("上架完成, inboundOrderId={}, lines={}, erpTenantId={}", order.getId(), merged.size(), erpTenantId);
	}

	// ==================== 可用库位（上架分配用）====================

	private static final String ZONE_STANDARD = "STANDARD";

	private static final String ZONE_DEFECTIVE = "DEFECTIVE";

	/** 品质→分区类型：良品(GOOD)入标准区，次品入不良品区。 */
	static String zoneTypeForQuality(String quality) {
		return GOOD.equals(normalizeQuality(quality)) ? ZONE_STANDARD : ZONE_DEFECTIVE;
	}

	/**
	 * 上架可选库位：本货主服务商租用货架上、空闲且分区匹配品质的库位（完全严格 + 库位独占 → 仅空库位）。
	 * @param inboundOrderId 入库单ID（据此解析 货主→父服务商→当前有效租用货架）
	 * @param quality        货物品质 GOOD/DAMAGED（决定可选分区）
	 */
	public List<AvailableLocationVO> listAvailableLocations(Long inboundOrderId, String quality) {
		assertPlatform();
		PurchaseInboundOrder order = purchaseInboundMapper.selectById(inboundOrderId);
		Assert.notNull(order, "入库单不存在");
		Long warehouseId = order.getWarehouseId();
		// 完全严格：只列本货主父服务商在本仓当前有效租用的货架上的库位；无租用货架 → 无候选
		Set<String> allowedRacks = resolveAllowedRacks(order);
		if (allowedRacks.isEmpty()) {
			return new ArrayList<>();
		}
		String zoneType = zoneTypeForQuality(quality);
		List<WmsZone> zones = wmsZoneService.listByWarehouse(warehouseId);
		Map<Long, WmsZone> zoneById = zones.stream().collect(Collectors.toMap(WmsZone::getId, z -> z, (a, b) -> a));
		// 候选分区仅按品质↔分区类型匹配（良品→STANDARD、次品→DEFECTIVE）；
		// 不能再按 zone.allocatable 过滤：allocatable=0 表示该区库存「出库不可拣」（不良品区正是如此），
		// 与「能否上架落位」无关，否则次品永远选不到不良品区（与 putaway 守卫只校验 zone_type 的口径也一致）。
		Set<Long> allowedZoneIds = zones.stream()
				.filter(z -> zoneType.equals(z.getZoneType()))
				.map(WmsZone::getId)
				.collect(Collectors.toSet());
		if (allowedZoneIds.isEmpty()) {
			return new ArrayList<>();
		}
		Set<String> occupied = new HashSet<>(physicalInventoryService.occupiedLocationCodes(warehouseId));
		return wmsLocationService.listByWarehouse(warehouseId).stream()
				.filter(l -> allowedRacks.contains(l.getRackNo()))
				.filter(l -> l.getZoneId() != null && allowedZoneIds.contains(l.getZoneId()))
				.filter(l -> !occupied.contains(l.getLocationCode()))
				.sorted(Comparator.comparing(WmsLocation::getRackNo, Comparator.nullsLast(String::compareTo))
						.thenComparing(l -> l.getColumnNo() == null ? Integer.MAX_VALUE : l.getColumnNo()))
				.map(l -> {
					AvailableLocationVO vo = new AvailableLocationVO();
					vo.setLocationId(l.getId());
					vo.setLocationCode(l.getLocationCode());
					vo.setZoneId(l.getZoneId());
					WmsZone z = zoneById.get(l.getZoneId());
					vo.setZoneName(z == null ? null : z.getZoneName());
					vo.setZoneType(z == null ? null : z.getZoneType());
					vo.setRackNo(l.getRackNo());
					vo.setColumnNo(l.getColumnNo());
					return vo;
				})
				.collect(Collectors.toList());
	}

	private void assertPlatform() {
		String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
		if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identityType)) {
			throw new BusinessException(WmsResultCode.INBOUND_OP_FORBIDDEN.getCode(),
					WmsResultCode.INBOUND_OP_FORBIDDEN.getMessage());
		}
	}

	/**
	 * 解析该入库单可上架的货架排：货主(erp_tenant_id) → 父服务商(parent_wms_tenant_id) →
	 * 该仓当前有效租用的货架排号。任一环缺失则返回空集（= 无可上架货架，上架被拦）。
	 */
	private Set<String> resolveAllowedRacks(PurchaseInboundOrder order) {
		Long erpTenantId = order.getErpTenantId();
		if (erpTenantId == null) {
			return Collections.emptySet();
		}
		SysTenant owner = sysTenantMapper.selectById(erpTenantId);
		Long operatorId = owner == null ? null : owner.getParentWmsTenantId();
		if (operatorId == null) {
			return Collections.emptySet();
		}
		return wmsRackAssignmentService.activeRackNos(order.getWarehouseId(), operatorId);
	}

	// ==================== 纯逻辑（便于单测）====================

	static final String GOOD = "GOOD";

	/**
	 * 合并上架行（同 SKU×库位×品质累加），品质空值归一为 GOOD。
	 */
	public static List<MergedLine> mergeLines(List<InboundPutawayDTO.PutawayLine> lines) {
		Map<String, MergedLine> map = new LinkedHashMap<>();
		for (InboundPutawayDTO.PutawayLine line : lines) {
			String quality = normalizeQuality(line.getQuality());
			String key = line.getSkuCode() + "|" + line.getLocationCode() + "|" + quality;
			MergedLine m = map.get(key);
			if (m == null) {
				m = new MergedLine(line.getSkuCode(), line.getLocationCode(), quality, 0, line.getZoneId());
				map.put(key, m);
			}
			m.quantity += line.getQuantity();
		}
		return new ArrayList<>(map.values());
	}

	/**
	 * 按 SKU 汇总上架数量。
	 */
	public static Map<String, Integer> sumBySku(List<MergedLine> merged) {
		Map<String, Integer> map = new LinkedHashMap<>();
		for (MergedLine line : merged) {
			map.merge(line.skuCode, line.quantity, Integer::sum);
		}
		return map;
	}

	/**
	 * 上架按 SKU 是否恰好覆盖收货数量（键集合一致且各数量相等）。
	 */
	public static boolean coversExactly(Map<String, Integer> receivedBySku, Map<String, Integer> putawayBySku) {
		return receivedBySku.equals(putawayBySku);
	}

	static String normalizeQuality(String quality) {
		if (quality == null || quality.isEmpty()) {
			return GOOD;
		}
		// M-7：品质白名单 + 大写归一。防非规范值（小写 damaged / DEFECTIVE 等）以 allocatable=0 落库后，
		// 聚合时既不满足 "DAMAGED" 精确匹配、又因 allocatable≠1 不计 available → 批次两桶双双落空、库存凭空消失。
		String q = quality.trim().toUpperCase();
		if (!GOOD.equals(q) && !"DAMAGED".equals(q)) {
			throw new BusinessException(400, "非法品质值：" + quality + "（仅允许 GOOD/DAMAGED）");
		}
		return q;
	}

	/**
	 * 合并后的上架行。
	 */
	public static class MergedLine {

		public final String skuCode;

		public final String locationCode;

		public final String quality;

		public int quantity;

		public final Long zoneId;

		public MergedLine(String skuCode, String locationCode, String quality, int quantity, Long zoneId) {
			this.skuCode = skuCode;
			this.locationCode = locationCode;
			this.quality = quality;
			this.quantity = quantity;
			this.zoneId = zoneId;
		}

	}

}
