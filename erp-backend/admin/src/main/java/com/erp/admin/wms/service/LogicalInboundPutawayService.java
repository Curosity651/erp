package com.erp.admin.wms.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.platform.finance.service.WarehouseBillingService;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.model.vo.SkuFileVO;
import com.erp.admin.product.service.SkuService;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.wms.mapper.PurchaseInboundItemMapper;
import com.erp.admin.wms.mapper.PurchaseInboundMapper;
import com.erp.admin.wms.mapper.WmsPutawayReceiptLineMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.dto.PutawayRecordDTO;
import com.erp.admin.wms.model.dto.LocationInventoryKey;
import com.erp.admin.wms.model.entity.PurchaseInboundOrder;
import com.erp.admin.wms.model.entity.PurchaseInboundOrderItem;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsPutawayReceiptLine;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.enums.PurchaseInboundStatus;
import com.erp.admin.wms.model.vo.LocationCapacityVO;
import com.erp.admin.wms.model.vo.PutawayRecordContextVO;
import com.erp.admin.wms.model.vo.PutawayReceiptLineVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LogicalInboundPutawayService {

	private static final String GOOD = "GOOD";

	private static final String DEFECTIVE = "DEFECTIVE";

	private final PurchaseInboundMapper inboundMapper;

	private final PurchaseInboundItemMapper itemMapper;

	private final WmsLocationService locationService;

	private final WmsZoneService zoneService;

	private final WmsRackAssignmentService rackAssignmentService;

	private final SysTenantMapper tenantMapper;

	private final SkuMapper skuMapper;

	private final LocationCapacityService capacityService;

	private final SkuService skuService;

	private final WarehouseSkuCodeService warehouseSkuCodeService;

	private final LocationInventoryService inventoryService;

	private final WmsPutawayReceiptLineMapper receiptMapper;

	private final WmsLocationMapper locationMapper;

	private final WarehouseBillingService billingService;

	private final PrincipalAttributeAccessor principalAccessor;

	public PutawayRecordContextVO context(Long inboundOrderId) {
		PurchaseInboundOrder order = requireReceivedOrder(inboundOrderId, false);
		Map<String, Integer> received = receivedBySku(order.getId());
		Map<String, Sku> skus = skuByCode(order.getErpTenantId(), received.keySet());
		SysTenant owner = tenantMapper.selectById(order.getErpTenantId());
		Assert.notNull(owner, "货主不存在");
		Long wmsTenantId = owner.getParentWmsTenantId();
		Assert.notNull(wmsTenantId, "货主未绑定WMS服务商");
		Set<String> allowedRacks = rackAssignmentService.activeRackNos(order.getWarehouseId(), wmsTenantId);
		Map<Long, WmsZone> zones = zoneService.listByWarehouse(order.getWarehouseId()).stream()
				.collect(Collectors.toMap(WmsZone::getId, Function.identity(), (left, right) -> left));
		PutawayRecordContextVO result = new PutawayRecordContextVO();
		result.setInboundOrderId(order.getId());
		result.setInboundNo(order.getInboundNo());
		result.setWarehouseId(order.getWarehouseId());
		result.setErpTenantId(order.getErpTenantId());
		result.setOwnerName(owner.getTenantName());
		for (Map.Entry<String, Integer> entry : received.entrySet()) {
			Sku sku = skus.get(entry.getKey());
			Assert.notNull(sku, "SKU不存在：" + entry.getKey());
			PutawayRecordContextVO.SkuSummaryVO item = new PutawayRecordContextVO.SkuSummaryVO();
			item.setSkuCode(sku.getSkuCode());
			item.setWarehouseSkuCode(warehouseSkuCodeService.build(order.getErpTenantId(), sku.getSkuCode()));
			item.setSkuName(sku.getChineseName());
			item.setImageUrl(firstImage(order.getErpTenantId(), sku.getId()));
			item.setReceivedQuantity(entry.getValue());
			item.setOuterLengthMm(sku.getOuterLengthMm());
			item.setOuterWidthMm(sku.getOuterWidthMm());
			item.setOuterHeightMm(sku.getOuterHeightMm());
			item.setOuterGrossWeightG(sku.getOuterGrossWeightG());
			result.getItems().add(item);
		}
		locationService.listByWarehouse(order.getWarehouseId()).stream()
				.filter(location -> {
					WmsZone zone = zones.get(location.getZoneId());
					return isPublicTemp(location, zone) || allowedRacks.contains(location.getRackNo());
				})
				.sorted(locationComparator())
				.map(location -> toLocationSummary(location, zones.get(location.getZoneId())))
				.forEach(result.getLocations()::add);
		return result;
	}

	@Transactional(rollbackFor = Exception.class)
	public List<PutawayReceiptLineVO> record(PutawayRecordDTO dto) {
		Assert.notNull(dto, "上架请求不能为空");
		PurchaseInboundOrder order = requireReceivedOrder(dto.getInboundOrderId(), true);
		List<PutawayRecordDTO.RecordLine> lines = dto.getLines();
		Assert.notEmpty(lines, "上架分配不能为空");
		lines = mergeAllocations(lines);
		Map<String, Integer> received = receivedBySku(order.getId());
		validateAllocationTotals(received, lines);
		Map<Long, WmsLocation> locations = locationService.listByWarehouse(order.getWarehouseId()).stream()
				.collect(Collectors.toMap(WmsLocation::getId, Function.identity()));
		Map<Long, WmsZone> zones = zoneService.listByWarehouse(order.getWarehouseId()).stream()
				.collect(Collectors.toMap(WmsZone::getId, Function.identity(), (left, right) -> left));
		SysTenant owner = tenantMapper.selectById(order.getErpTenantId());
		Assert.notNull(owner, "货主不存在");
		Long wmsTenantId = owner.getParentWmsTenantId();
		Assert.notNull(wmsTenantId, "货主未绑定WMS服务商");
		Set<String> allowedRacks = rackAssignmentService.activeRackNos(order.getWarehouseId(), wmsTenantId);
		Map<String, Sku> skus = skuByCode(order.getErpTenantId(), received.keySet());
		Map<Long, List<PutawayRecordDTO.RecordLine>> byLocation = lines.stream()
				.collect(Collectors.groupingBy(PutawayRecordDTO.RecordLine::getLocationId));
		for (Long locationId : byLocation.keySet().stream().sorted().collect(Collectors.toList())) {
			WmsLocation locked = locationMapper.selectLogicalByIdForUpdate(locationId);
			Assert.notNull(locked, "目标库位不存在：" + locationId);
			locations.put(locationId, locked);
		}

		for (Map.Entry<Long, List<PutawayRecordDTO.RecordLine>> entry : byLocation.entrySet()) {
			WmsLocation location = locations.get(entry.getKey());
			Assert.notNull(location, "目标库位不存在：" + entry.getKey());
			WmsZone zone = zones.get(location.getZoneId());
			validateTargetLocation(order.getWarehouseId(), location, zone);
			Assert.isTrue(isPublicTemp(location, zone) || allowedRacks.contains(location.getRackNo()),
					"目标库位不属于当前服务商且不是公共暂存区：" + location.getLocationCode());
			List<LocationCapacityService.PlacementLine> additions = new ArrayList<>();
			boolean capacityCalculable = true;
			for (PutawayRecordDTO.RecordLine line : entry.getValue()) {
				String quality = normalizeQuality(line.getQuality());
				validateTargetType(quality, location, zone.getZoneType());
				Sku sku = skus.get(line.getSkuCode());
				Assert.notNull(sku, "SKU不存在：" + line.getSkuCode());
				try {
					additions.add(LocationCapacityService.fromSku(sku, line.getQuantity()));
				}
				catch (IllegalArgumentException ex) {
					capacityCalculable = false;
				}
			}
			LocationCapacityVO capacity = capacityCalculable ? tryEvaluate(location.getId(), additions) : null;
			if (capacity != null) {
				for (PutawayRecordDTO.RecordLine line : entry.getValue()) {
					validateCapacityOverride(capacity.isVolumeAllowed(), capacity.isWeightAllowed(),
							capacity.isSkuKindsAllowed(), line.getCapacityOverrideReason());
				}
			}
		}

		for (PutawayRecordDTO.RecordLine line : lines) {
			LocationInventoryKey key = new LocationInventoryKey();
			key.setTenantId(TenantContext.BLOCK_TENANT_ID);
			key.setWmsTenantId(wmsTenantId);
			key.setErpTenantId(order.getErpTenantId());
			key.setWarehouseId(order.getWarehouseId());
			key.setLocationId(line.getLocationId());
			key.setSkuCode(line.getSkuCode());
			key.setQuality(normalizeQuality(line.getQuality()));
			inventoryService.increase(key, line.getQuantity(),
					com.erp.admin.wms.model.dto.InventoryMutationContext.builder()
							.eventType(com.erp.admin.wms.model.enums.InventoryEventType.INBOUND_PUTAWAY)
							.sourceType("PURCHASE_INBOUND").sourceId(order.getId())
							.sourceNo(order.getInboundNo()).operatorId(principalAccessor.getUserId())
							.reason("入库上架")
							.idempotencyKey("inbound-putaway:" + order.getId() + ":" + line.getLocationId()
									+ ":" + line.getSkuCode() + ":" + key.getQuality()).build());
			saveReceipt(order.getId(), locations.get(line.getLocationId()), line);
		}

		List<PurchaseInboundOrderItem> items = itemMapper.selectByInboundOrderId(order.getId());
		billingService.recordInbound(order, items, dto.getConfirmedVolumeCbm(), dto.getAfterHours(),
				dto.getAfterHoursReason());
		int claimed = inboundMapper.casOrderStatus(order.getId(), PurchaseInboundStatus.RECEIVED.name(),
				PurchaseInboundStatus.COMPLETED.name());
		if (claimed != 1) throw new BusinessException(409, "入库单状态已变化，请刷新后重试");
		Assert.isTrue(inboundMapper.recordPutawayOperator(order.getId(), principalAccessor.getUserId(),
				LocalDateTime.now()) == 1, "上架操作员记录失败");
		return receiptMapper.selectByInboundOrderId(order.getId());
	}

	public static void validateAllocationTotals(Map<String, Integer> received,
			List<PutawayRecordDTO.RecordLine> lines) {
		Map<String, Integer> allocated = lines.stream().collect(Collectors.toMap(
				PutawayRecordDTO.RecordLine::getSkuCode, PutawayRecordDTO.RecordLine::getQuantity, Integer::sum));
		Assert.isTrue(received.equals(allocated), "上架数量必须与实收数量完全一致");
	}

	public static List<PutawayRecordDTO.RecordLine> mergeAllocations(List<PutawayRecordDTO.RecordLine> lines) {
		Map<String, PutawayRecordDTO.RecordLine> merged = new LinkedHashMap<>();
		for (PutawayRecordDTO.RecordLine source : lines) {
			Assert.notNull(source.getLocationId(), "目标库位不能为空");
			Assert.hasText(source.getSkuCode(), "SKU编码不能为空");
			Assert.isTrue(source.getQuantity() != null && source.getQuantity() > 0, "上架数量必须大于0");
			String quality = StringUtils.hasText(source.getQuality()) ? source.getQuality().toUpperCase() : GOOD;
			String key = source.getLocationId() + "|" + source.getSkuCode() + "|" + quality;
			PutawayRecordDTO.RecordLine target = merged.get(key);
			if (target == null) {
				target = new PutawayRecordDTO.RecordLine();
				target.setLocationId(source.getLocationId());
				target.setSkuCode(source.getSkuCode());
				target.setQuality(quality);
				target.setQuantity(source.getQuantity());
				target.setCapacityOverrideReason(source.getCapacityOverrideReason());
				merged.put(key, target);
			}
			else {
				target.setQuantity(target.getQuantity() + source.getQuantity());
				if (!StringUtils.hasText(target.getCapacityOverrideReason())) {
					target.setCapacityOverrideReason(source.getCapacityOverrideReason());
				}
			}
		}
		return new ArrayList<>(merged.values());
	}

	public static void validateCapacityOverride(Boolean volumeAllowed, Boolean weightAllowed,
			Boolean skuKindsAllowed, String reason) {
		if (Boolean.FALSE.equals(volumeAllowed) || Boolean.FALSE.equals(weightAllowed)) {
			Assert.hasText(reason, "库位体积或承重超限时必须填写现场说明");
		}
	}

	public static void validateTargetType(String quality, WmsLocation location, String zoneType) {
		String type = StringUtils.hasText(zoneType) ? zoneType : location.getLocationType();
		if (DEFECTIVE.equalsIgnoreCase(quality) || "DAMAGED".equalsIgnoreCase(quality)) {
			Assert.isTrue(DEFECTIVE.equalsIgnoreCase(type), "不良品只能放入不良品区");
		}
		else {
			Assert.isTrue(!DEFECTIVE.equalsIgnoreCase(type), "良品不能放入不良品区");
		}
	}

	private PutawayRecordContextVO.LocationSummaryVO toLocationSummary(WmsLocation location, WmsZone zone) {
		PutawayRecordContextVO.LocationSummaryVO result = new PutawayRecordContextVO.LocationSummaryVO();
		result.setLocationId(location.getId());
		result.setLocationCode(location.getLocationCode());
		result.setRackNo(location.getRackNo());
		result.setZoneId(location.getZoneId());
		result.setZoneName(zone == null ? null : zone.getZoneName());
		result.setZoneType(zone == null ? null : zone.getZoneType());
		result.setPublicShared(location.getPublicShared());
		LocationCapacityVO capacity = tryEvaluate(location.getId(), Collections.emptyList());
		if (capacity == null) {
			result.setCapacityCalculable(false);
			return result;
		}
		result.setCapacityCalculable(true);
		result.setCapacityVolumeMm3(capacity.getCapacityVolumeMm3());
		result.setOccupiedVolumeMm3(capacity.getOccupiedVolumeMm3());
		result.setOccupiedWeightGrams(capacity.getOccupiedWeightGrams());
		result.setMaxWeightGrams(capacity.getMaxWeightGrams());
		result.setSkuKindCount(capacity.getSkuKindCount());
		result.setMaxSkuKinds(capacity.getMaxSkuKinds());
		result.setVolumeAllowed(capacity.isVolumeAllowed());
		result.setWeightAllowed(capacity.isWeightAllowed());
		result.setSkuKindsAllowed(capacity.isSkuKindsAllowed());
		result.setUtilizationPercent(capacity.getUtilizationPercent());
		return result;
	}

	private LocationCapacityVO tryEvaluate(Long locationId, List<LocationCapacityService.PlacementLine> additions) {
		try {
			return capacityService.evaluate(locationId, additions);
		}
		catch (IllegalArgumentException | ArithmeticException ex) {
			return null;
		}
	}

	private String firstImage(Long erpTenantId, Long skuId) {
		Map<String, List<SkuFileVO>> files = TenantContext.runAs(erpTenantId, () -> skuService.getSkuFilesMap(skuId));
		for (String type : new String[] { "actual_image", "platform_image" }) {
			List<SkuFileVO> candidates = files == null ? null : files.get(type);
			if (candidates != null && !candidates.isEmpty()) return candidates.get(0).getFileUrl();
		}
		return null;
	}

	private Comparator<WmsLocation> locationComparator() {
		return Comparator.comparing((WmsLocation location) -> rackPrefix(location.getRackNo()))
				.thenComparingInt(location -> rackNumber(location.getRackNo()))
				.thenComparing(location -> location.getColumnNo() == null ? Integer.MAX_VALUE : location.getColumnNo())
				.thenComparing(WmsLocation::getLocationCode, Comparator.nullsLast(String::compareToIgnoreCase));
	}

	private String rackPrefix(String rackNo) {
		return StringUtils.hasText(rackNo) ? rackNo.replaceAll("\\d", "").toUpperCase() : "";
	}

	private int rackNumber(String rackNo) {
		if (!StringUtils.hasText(rackNo)) return Integer.MAX_VALUE;
		String digits = rackNo.replaceAll("\\D", "");
		if (!StringUtils.hasText(digits)) return Integer.MAX_VALUE;
		try {
			return Integer.parseInt(digits);
		}
		catch (NumberFormatException ex) {
			return Integer.MAX_VALUE;
		}
	}

	private PurchaseInboundOrder requireReceivedOrder(Long id, boolean lock) {
		PurchaseInboundOrder order = lock ? inboundMapper.selectByIdForUpdate(id) : inboundMapper.selectById(id);
		Assert.notNull(order, "入库单不存在");
		Assert.isTrue(PurchaseInboundStatus.RECEIVED.name().equals(order.getOrderStatus()),
				"只有已收货的入库单可以上架");
		return order;
	}

	private Map<String, Integer> receivedBySku(Long inboundOrderId) {
		return itemMapper.selectByInboundOrderId(inboundOrderId).stream()
				.filter(item -> item.getActualQuantity() != null && item.getActualQuantity() > 0)
				.collect(Collectors.toMap(PurchaseInboundOrderItem::getSkuCode,
						PurchaseInboundOrderItem::getActualQuantity, Integer::sum, LinkedHashMap::new));
	}

	private Map<String, Sku> skuByCode(Long erpTenantId, Set<String> codes) {
		if (codes.isEmpty()) return Collections.emptyMap();
		return TenantContext.runAs(erpTenantId, () -> skuMapper.selectBySkuCodes(codes).stream()
				.collect(Collectors.toMap(Sku::getSkuCode, Function.identity(), (left, right) -> left)));
	}

	public static boolean isPublicTemp(WmsLocation location, WmsZone zone) {
		return location != null && zone != null && "TEMP".equalsIgnoreCase(zone.getZoneType())
				&& Integer.valueOf(1).equals(location.getPublicShared());
	}

	public static void validateTargetLocation(Long warehouseId, WmsLocation location, WmsZone zone) {
		Assert.notNull(location, "目标库位不存在");
		Assert.isTrue(warehouseId.equals(location.getWarehouseId()),
				"目标库位不属于入库单仓库：" + location.getLocationCode());
		Assert.notNull(zone, "目标库位分区不存在或不属于入库单仓库：" + location.getLocationCode());
		Assert.isTrue(warehouseId.equals(zone.getWarehouseId()),
				"目标库位分区不属于入库单仓库：" + location.getLocationCode());
	}

	private String normalizeQuality(String quality) {
		return StringUtils.hasText(quality) ? quality.toUpperCase() : GOOD;
	}

	private void saveReceipt(Long inboundOrderId, WmsLocation location, PutawayRecordDTO.RecordLine line) {
		WmsPutawayReceiptLine receipt = new WmsPutawayReceiptLine();
		receipt.setInboundOrderId(inboundOrderId);
		receipt.setLocationId(location.getId());
		receipt.setLocationCode(location.getLocationCode());
		receipt.setSlotCode(location.getLocationCode());
		receipt.setSkuCode(line.getSkuCode());
		receipt.setQuality(normalizeQuality(line.getQuality()));
		receipt.setQuantity(line.getQuantity());
		receipt.setOverrideReason(line.getCapacityOverrideReason());
		receipt.setCreateTime(LocalDateTime.now());
		Assert.isTrue(receiptMapper.insert(receipt) == 1, "上架单保存失败");
	}

}
