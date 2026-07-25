package com.erp.admin.wms.service;

import com.erp.admin.platform.finance.service.WarehouseBillingService;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.mapper.WmsSkuLookupMapper;
import com.erp.admin.wms.model.entity.PurchaseInboundOrder;
import com.erp.admin.wms.model.entity.PurchaseInboundOrderItem;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.vo.InboundPutawayPlanVO;
import com.erp.admin.wms.model.vo.PalletSlotVO;
import com.erp.admin.wms.model.vo.PalletSummaryVO;
import com.erp.admin.wms.model.vo.SkuLookupVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Builds manual putaway context: received goods, capacity reference and eligible slots. */
@Service
@RequiredArgsConstructor
public class InboundPalletPlanningService {

    private final WarehouseService warehouseService;
    private final WmsPalletService palletService;
    private final WmsPhysicalInventoryMapper physicalInventoryMapper;
    private final WmsSkuLookupMapper skuLookupMapper;
    private final WarehouseBillingService warehouseBillingService;

    public InboundPutawayPlanVO plan(PurchaseInboundOrder order, List<PurchaseInboundOrderItem> items,
            Set<String> allowedRacks) {
        Warehouse warehouse = warehouseService.getById(order.getWarehouseId());
        Assert.notNull(warehouse, "仓库不存在");
        Long ownerId = order.getErpTenantId();
        InboundPutawayPlanVO result = new InboundPutawayPlanVO();
        result.setInboundOrderId(order.getId());
        result.setWarehouseId(order.getWarehouseId());
        WarehouseBillingService.VolumeQuote volumeQuote = warehouseBillingService
                .quoteInbound(ownerId, items);
        result.setCalculatedVolumeCbm(volumeQuote.getCalculatedVolumeCbm());
        result.setVolumeMissingSkuCodes(volumeQuote.getMissingSkuCodes());

        List<PalletSlotVO> allSlots = palletService.listSlots(order.getWarehouseId()).stream()
                .filter(slot -> allowedRacks.contains(slot.getRackNo()))
                .collect(Collectors.toList());
        List<PalletSlotVO> freeSlots = allSlots.stream()
                .filter(slot -> WmsPalletService.EMPTY.equals(slot.getSlotStatus()))
                .collect(Collectors.toList());
        result.setSlotCandidates(freeSlots);
        int sequence = 1;
        List<InboundPutawayPlanVO.PalletPlan> plans = new ArrayList<>();

        for (PurchaseInboundOrderItem inboundItem : items) {
            int quantity = inboundItem.getActualQuantity() == null ? 0 : inboundItem.getActualQuantity();
            if (quantity <= 0) {
                continue;
            }
            SkuLookupVO sku = skuLookupMapper.findByTenantAndSku(ownerId, inboundItem.getSkuCode());
            Capacity capacity = capacityFor(warehouse, sku);
            if (capacity.quantity <= 0) {
                result.getWarnings().add("SKU " + inboundItem.getSkuCode()
                        + " 缺少每托数量或有效尺寸/重量，本次需要人工确认容量");
            }

            InboundPutawayPlanVO.PalletPlan plan = createPlan("MANUAL-" + sequence++, ownerId,
                    inboundItem, quantity, capacity, null, false);
            plan.setSlotCode("");
            plan.setLocationCode("");
            if (capacity.quantity > 0 && quantity > capacity.quantity) {
                result.getWarnings().add("SKU " + inboundItem.getSkuCode() + " 超过参考单托数量 "
                        + capacity.quantity + "，请人工拆分为多个托盘");
            }
            plans.add(plan);
        }

        result.setPallets(plans);
        return result;
    }

    private void assignSlots(List<InboundPutawayPlanVO.PalletPlan> plans, List<PalletSlotVO> allSlots,
            List<PalletSlotVO> freeSlots) {
        Set<Long> used = new LinkedHashSet<>();
        for (InboundPutawayPlanVO.PalletPlan plan : plans) {
            if (Boolean.TRUE.equals(plan.getExistingPallet())) {
                PalletSlotVO existing = allSlots.stream()
                        .filter(slot -> plan.getSlotCode().equals(slot.getSlotCode())).findFirst().orElse(null);
                if (existing != null) {
                    applySlot(plan, existing);
                }
                continue;
            }
            Comparator<PalletSlotVO> levelOrder = Boolean.TRUE.equals(plan.getWholePalletEligible())
                    ? Comparator.comparing(PalletSlotVO::getLevelNo).reversed()
                    : Comparator.comparing(PalletSlotVO::getLevelNo);
            PalletSlotVO chosen = freeSlots.stream().filter(slot -> !used.contains(slot.getSlotId()))
                    .sorted(levelOrder.thenComparing(PalletSlotVO::getSlotCode)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("可用托盘层位不足，无法完成本次上架规划"));
            used.add(chosen.getSlotId());
            applySlot(plan, chosen);
        }
    }

    private void applySlot(InboundPutawayPlanVO.PalletPlan plan, PalletSlotVO slot) {
        plan.setSlotCode(slot.getSlotCode());
        plan.setLocationCode(slot.getLocationCode());
        plan.setLevelNo(slot.getLevelNo());
    }

    private InboundPutawayPlanVO.PalletPlan createPlan(String key, Long ownerId,
            PurchaseInboundOrderItem inboundItem, int quantity, Capacity capacity, String slotCode, boolean full) {
        InboundPutawayPlanVO.PalletPlan plan = new InboundPutawayPlanVO.PalletPlan();
        plan.setPalletKey(key);
        plan.setExistingPallet(false);
        plan.setPalletType(full ? "SINGLE_FULL" : "SINGLE_PARTIAL");
        plan.setQuality("GOOD");
        plan.setSlotCode(slotCode);
        plan.setCapacityPercent(capacity.quantity > 0 ? percent(quantity, capacity.quantity) : null);
        plan.setCapacitySource(capacity.source);
        plan.setEstimatedWeightKg(capacity.unitWeightKg == null ? null
                : capacity.unitWeightKg.multiply(BigDecimal.valueOf(quantity)).setScale(3, RoundingMode.HALF_UP));
        plan.setWholePalletEligible(full);
        plan.getItems().add(createItem(ownerId, inboundItem, quantity, capacity.quantity));
        return plan;
    }

    private InboundPutawayPlanVO.PalletPlanItem createItem(Long ownerId,
            PurchaseInboundOrderItem inboundItem, int quantity, int quantityPerPallet) {
        InboundPutawayPlanVO.PalletPlanItem item = new InboundPutawayPlanVO.PalletPlanItem();
        item.setErpTenantId(ownerId);
        item.setSkuCode(inboundItem.getSkuCode());
        item.setQuantity(quantity);
        item.setQuantityPerPallet(quantityPerPallet > 0 ? quantityPerPallet : null);
        return item;
    }

    private Capacity capacityFor(Warehouse warehouse, SkuLookupVO sku) {
        if (sku == null) {
            return new Capacity(0, "MANUAL_REQUIRED", null);
        }
        List<Integer> safetyCaps = new ArrayList<>();
        if (sku.getQuantityPerPallet() != null && sku.getQuantityPerPallet() > 0) {
            safetyCaps.add(sku.getQuantityPerPallet());
        }
        BigDecimal unitVolume = unitVolumeCbm(sku);
        if (unitVolume != null && unitVolume.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal palletVolume = BigDecimal.valueOf(value(warehouse.getDefaultPalletLengthMm(), 1200))
                    .multiply(BigDecimal.valueOf(value(warehouse.getDefaultPalletWidthMm(), 1000)))
                    .multiply(BigDecimal.valueOf(value(warehouse.getDefaultPalletHeightMm(), 1600)))
                    .divide(new BigDecimal("1000000000"), 9, RoundingMode.DOWN)
                    .multiply(warehouse.getDefaultPalletUtilization() == null
                            ? new BigDecimal("0.85") : warehouse.getDefaultPalletUtilization());
            safetyCaps.add(palletVolume.divide(unitVolume, 0, RoundingMode.DOWN).intValue());
        }
        BigDecimal unitWeight = weightKg(sku);
        if (unitWeight != null && unitWeight.compareTo(BigDecimal.ZERO) > 0
                && warehouse.getDefaultPalletMaxWeightKg() != null) {
            safetyCaps.add(warehouse.getDefaultPalletMaxWeightKg()
                    .divide(unitWeight, 0, RoundingMode.DOWN).intValue());
        }
        int quantity = safetyCaps.stream().filter(value -> value > 0).min(Integer::compareTo).orElse(0);
        String source = sku.getQuantityPerPallet() != null && sku.getQuantityPerPallet() > 0
                ? "SKU_QTY" : quantity > 0 ? "VOLUME_WEIGHT" : "MANUAL_REQUIRED";
        return new Capacity(quantity, source, unitWeight);
    }

    private BigDecimal unitVolumeCbm(SkuLookupVO sku) {
        if (sku.getPackageLength() == null || sku.getPackageWidth() == null || sku.getPackageHeight() == null
                || sku.getPackageLength().compareTo(BigDecimal.ZERO) <= 0
                || sku.getPackageWidth().compareTo(BigDecimal.ZERO) <= 0
                || sku.getPackageHeight().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal factor = dimensionFactor(sku.getPackageUnit());
        return sku.getPackageLength().multiply(sku.getPackageWidth()).multiply(sku.getPackageHeight())
                .multiply(factor.pow(3)).setScale(9, RoundingMode.HALF_UP);
    }

    private BigDecimal dimensionFactor(String unit) {
        if ("MM".equalsIgnoreCase(unit)) return new BigDecimal("0.001");
        if ("CM".equalsIgnoreCase(unit)) return new BigDecimal("0.01");
        return BigDecimal.ONE;
    }

    private BigDecimal weightKg(SkuLookupVO sku) {
        if (sku.getWeight() == null || sku.getWeight().compareTo(BigDecimal.ZERO) <= 0) return null;
        if ("G".equalsIgnoreCase(sku.getWeightUnit())) return sku.getWeight().divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
        return sku.getWeight();
    }

    private BigDecimal percent(int quantity, int capacity) {
        if (capacity <= 0) return null;
        return BigDecimal.valueOf(quantity).multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(capacity), 2, RoundingMode.HALF_UP)
                .min(new BigDecimal("100"));
    }

    private int maxKinds(Warehouse warehouse) {
        return warehouse.getMaxSkuKindsPerPallet() == null ? 4 : warehouse.getMaxSkuKindsPerPallet();
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private int value(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private static class Capacity {
        private final int quantity;
        private final String source;
        private final BigDecimal unitWeightKg;

        private Capacity(int quantity, String source, BigDecimal unitWeightKg) {
            this.quantity = quantity;
            this.source = source;
            this.unitWeightKg = unitWeightKg;
        }
    }
}
