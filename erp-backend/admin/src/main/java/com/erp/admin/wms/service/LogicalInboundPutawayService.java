package com.erp.admin.wms.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.wms.mapper.PurchaseInboundItemMapper;
import com.erp.admin.wms.mapper.PurchaseInboundMapper;
import com.erp.admin.wms.mapper.WmsPutawayReceiptLineMapper;
import com.erp.admin.wms.model.dto.InboundPutawayDTO;
import com.erp.admin.wms.model.dto.LocationInventoryKey;
import com.erp.admin.wms.model.entity.PurchaseInboundOrder;
import com.erp.admin.wms.model.entity.PurchaseInboundOrderItem;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsPutawayReceiptLine;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.enums.PurchaseInboundStatus;
import com.erp.admin.wms.model.vo.LocationCapacityVO;
import com.erp.admin.wms.model.vo.LogicalInboundPutawayPlanVO;
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

	private final LocationRecommendationService recommendationService;

	private final LocationCapacityService capacityService;

	private final LocationInventoryService inventoryService;

	private final WmsPutawayReceiptLineMapper receiptMapper;

	private final WarehouseBillingService billingService;

	private final PrincipalAttributeAccessor principalAccessor;

	public LogicalInboundPutawayPlanVO plan(Long inboundOrderId) {
		PurchaseInboundOrder order = requireReceivedOrder(inboundOrderId, false);
		Map<String, Integer> received = receivedBySku(order.getId());
		Map<String, Sku> skus = skuByCode(received.keySet());
		SysTenant owner = tenantMapper.selectById(order.getErpTenantId());
		Assert.notNull(owner, "货主不存在");
		Long wmsTenantId = owner.getParentWmsTenantId();
		Assert.notNull(wmsTenantId, "货主未绑定WMS服务商");
		Set<String> allowedRacks = rackAssignmentService.activeRackNos(order.getWarehouseId(), wmsTenantId);
		Map<Long, WmsLocation> locations = locationService.listByWarehouse(order.getWarehouseId()).stream()
				.collect(Collectors.toMap(WmsLocation::getId, Function.identity()));
		Map<Long, String> zoneTypes = zoneService.listByWarehouse(order.getWarehouseId()).stream()
				.collect(Collectors.toMap(WmsZone::getId, WmsZone::getZoneType, (left, right) -> left));
		LogicalInboundPutawayPlanVO result = new LogicalInboundPutawayPlanVO();
		result.setInboundOrderId(order.getId());
		result.setWarehouseId(order.getWarehouseId());
		for (Map.Entry<String, Integer> entry : received.entrySet()) {
			Sku sku = skus.get(entry.getKey());
			Assert.notNull(sku, "SKU不存在：" + entry.getKey());
			LogicalInboundPutawayPlanVO.SkuPlanVO item = new LogicalInboundPutawayPlanVO.SkuPlanVO();
			item.setSkuCode(sku.getSkuCode());
			item.setSkuName(sku.getChineseName());
			item.setReceivedQuantity(entry.getValue());
			item.setOuterLengthMm(sku.getOuterLengthMm());
			item.setOuterWidthMm(sku.getOuterWidthMm());
			item.setOuterHeightMm(sku.getOuterHeightMm());
			item.setOuterGrossWeightG(sku.getOuterGrossWeightG());
			item.setRecommendations(recommendationService.recommend(order.getWarehouseId(), order.getErpTenantId(),
					sku.getSkuCode(), entry.getValue(), GOOD).stream().filter(candidate -> {
					WmsLocation location = locations.get(candidate.getLocationId());
					if (location == null || !(isPublicTemp(location) || allowedRacks.contains(location.getRackNo()))) {
						return false;
					}
					String zoneType = zoneTypes.get(location.getZoneId());
					candidate.setZoneType(zoneType);
					return true;
				}).collect(Collectors.toList()));
			result.getItems().add(item);
		}
		return result;
	}

	@Transactional(rollbackFor = Exception.class)
	public List<PutawayReceiptLineVO> putaway(InboundPutawayDTO dto) {
		Assert.notNull(dto, "上架请求不能为空");
		PurchaseInboundOrder order = requireReceivedOrder(dto.getInboundOrderId(), true);
		List<InboundPutawayDTO.PutawayLine> lines = dto.getLines();
		Assert.notEmpty(lines, "上架分配不能为空");
		lines = mergeAllocations(lines);
		Map<String, Integer> received = receivedBySku(order.getId());
		validateAllocationTotals(received, lines);
		Map<Long, WmsLocation> locations = locationService.listByWarehouse(order.getWarehouseId()).stream()
				.collect(Collectors.toMap(WmsLocation::getId, Function.identity()));
		Map<Long, String> zoneTypes = zoneService.listByWarehouse(order.getWarehouseId()).stream()
				.collect(Collectors.toMap(WmsZone::getId, WmsZone::getZoneType, (left, right) -> left));
		SysTenant owner = tenantMapper.selectById(order.getErpTenantId());
		Assert.notNull(owner, "货主不存在");
		Long wmsTenantId = owner.getParentWmsTenantId();
		Assert.notNull(wmsTenantId, "货主未绑定WMS服务商");
		Set<String> allowedRacks = rackAssignmentService.activeRackNos(order.getWarehouseId(), wmsTenantId);
		Map<String, Sku> skus = skuByCode(received.keySet());
		Map<Long, List<InboundPutawayDTO.PutawayLine>> byLocation = lines.stream()
				.collect(Collectors.groupingBy(InboundPutawayDTO.PutawayLine::getLocationId));

		for (Map.Entry<Long, List<InboundPutawayDTO.PutawayLine>> entry : byLocation.entrySet()) {
			WmsLocation location = locations.get(entry.getKey());
			Assert.notNull(location, "目标库位不存在：" + entry.getKey());
			Assert.isTrue(isPublicTemp(location) || allowedRacks.contains(location.getRackNo()),
					"目标库位不属于当前服务商且不是公共暂存区：" + location.getLocationCode());
			List<LocationCapacityService.PlacementLine> additions = new ArrayList<>();
			for (InboundPutawayDTO.PutawayLine line : entry.getValue()) {
				String quality = normalizeQuality(line.getQuality());
				validateTargetType(quality, location, zoneTypes.get(location.getZoneId()));
				Sku sku = skus.get(line.getSkuCode());
				Assert.notNull(sku, "SKU不存在：" + line.getSkuCode());
				additions.add(LocationCapacityService.fromSku(sku, line.getQuantity()));
			}
			LocationCapacityVO capacity = capacityService.evaluate(location.getId(), additions);
			Assert.isTrue(capacity.isWeightAllowed(), "目标库位承重不足：" + location.getLocationCode());
			Assert.isTrue(capacity.isSkuKindsAllowed(), "目标库位SKU种类数超限：" + location.getLocationCode());
			if (!capacity.isVolumeAllowed()) {
				Assert.isTrue(entry.getValue().stream().allMatch(line -> StringUtils.hasText(line.getOverrideReason())),
						"超过体积推荐时必须填写人工覆盖原因：" + location.getLocationCode());
			}
		}

		for (InboundPutawayDTO.PutawayLine line : lines) {
			LocationInventoryKey key = new LocationInventoryKey();
			key.setTenantId(TenantContext.BLOCK_TENANT_ID);
			key.setWmsTenantId(wmsTenantId);
			key.setErpTenantId(order.getErpTenantId());
			key.setWarehouseId(order.getWarehouseId());
			key.setLocationId(line.getLocationId());
			key.setSkuCode(line.getSkuCode());
			key.setQuality(normalizeQuality(line.getQuality()));
			inventoryService.increase(key, line.getQuantity());
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
			List<InboundPutawayDTO.PutawayLine> lines) {
		Map<String, Integer> allocated = lines.stream().collect(Collectors.toMap(
				InboundPutawayDTO.PutawayLine::getSkuCode, InboundPutawayDTO.PutawayLine::getQuantity, Integer::sum));
		Assert.isTrue(received.equals(allocated), "上架数量必须与实收数量完全一致");
	}

	public static List<InboundPutawayDTO.PutawayLine> mergeAllocations(List<InboundPutawayDTO.PutawayLine> lines) {
		Map<String, InboundPutawayDTO.PutawayLine> merged = new LinkedHashMap<>();
		for (InboundPutawayDTO.PutawayLine source : lines) {
			Assert.notNull(source.getLocationId(), "目标库位不能为空");
			Assert.hasText(source.getSkuCode(), "SKU编码不能为空");
			Assert.isTrue(source.getQuantity() != null && source.getQuantity() > 0, "上架数量必须大于0");
			String quality = StringUtils.hasText(source.getQuality()) ? source.getQuality().toUpperCase() : GOOD;
			String key = source.getLocationId() + "|" + source.getSkuCode() + "|" + quality;
			InboundPutawayDTO.PutawayLine target = merged.get(key);
			if (target == null) {
				target = new InboundPutawayDTO.PutawayLine();
				target.setLocationId(source.getLocationId());
				target.setSkuCode(source.getSkuCode());
				target.setQuality(quality);
				target.setQuantity(source.getQuantity());
				target.setOverrideReason(source.getOverrideReason());
				merged.put(key, target);
			}
			else {
				target.setQuantity(target.getQuantity() + source.getQuantity());
				if (!StringUtils.hasText(target.getOverrideReason())) {
					target.setOverrideReason(source.getOverrideReason());
				}
			}
		}
		return new ArrayList<>(merged.values());
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

	private Map<String, Sku> skuByCode(Set<String> codes) {
		if (codes.isEmpty()) return Collections.emptyMap();
		return skuMapper.selectBySkuCodes(codes).stream()
				.collect(Collectors.toMap(Sku::getSkuCode, Function.identity(), (left, right) -> left));
	}

	private boolean isPublicTemp(WmsLocation location) {
		return "TEMP".equalsIgnoreCase(location.getLocationType()) && Integer.valueOf(1).equals(location.getPublicShared());
	}

	private String normalizeQuality(String quality) {
		return StringUtils.hasText(quality) ? quality.toUpperCase() : GOOD;
	}

	private void saveReceipt(Long inboundOrderId, WmsLocation location, InboundPutawayDTO.PutawayLine line) {
		WmsPutawayReceiptLine receipt = new WmsPutawayReceiptLine();
		receipt.setInboundOrderId(inboundOrderId);
		receipt.setLocationId(location.getId());
		receipt.setLocationCode(location.getLocationCode());
		receipt.setSlotCode(location.getLocationCode());
		receipt.setSkuCode(line.getSkuCode());
		receipt.setQuality(normalizeQuality(line.getQuality()));
		receipt.setQuantity(line.getQuantity());
		receipt.setOverrideReason(line.getOverrideReason());
		receipt.setCreateTime(LocalDateTime.now());
		Assert.isTrue(receiptMapper.insert(receipt) == 1, "上架单保存失败");
	}

}
