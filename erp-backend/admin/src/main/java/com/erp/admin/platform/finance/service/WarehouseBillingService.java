package com.erp.admin.platform.finance.service;

import com.erp.admin.platform.finance.mapper.WmsBillingRecordMapper;
import com.erp.admin.platform.finance.mapper.WmsFeeRateCardMapper;
import com.erp.admin.platform.finance.model.entity.WmsBillingRecord;
import com.erp.admin.platform.finance.model.entity.WmsFeeRateCard;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.wms.mapper.WmsPalletMapper;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.mapper.WmsSkuLookupMapper;
import com.erp.admin.wms.model.dto.ShipDTO;
import com.erp.admin.wms.model.entity.PurchaseInboundOrder;
import com.erp.admin.wms.model.entity.PurchaseInboundOrderItem;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.WmsOutboundPickAllocation;
import com.erp.admin.wms.model.entity.WmsPallet;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.vo.OutboundHandlingPreviewVO;
import com.erp.admin.wms.model.vo.SkuLookupVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseBillingService {

    public static final String INBOUND_CBM = "INBOUND_CBM";
    public static final String AFTER_HOURS_SURCHARGE = "AFTER_HOURS_SURCHARGE";
    public static final String OUTBOUND_FULL_PALLET = "OUTBOUND_FULL_PALLET";
    public static final String OUTBOUND_LARGE_BOX = "OUTBOUND_LARGE_BOX";
    public static final String OUTBOUND_SMALL_ITEM = "OUTBOUND_SMALL_ITEM";

    private final WmsFeeRateCardMapper rateMapper;
    private final WmsBillingRecordMapper billingMapper;
    private final WmsSkuLookupMapper skuLookupMapper;
    private final WmsPhysicalInventoryMapper physicalInventoryMapper;
    private final WmsPalletMapper palletMapper;
    private final SysTenantMapper tenantMapper;

    public VolumeQuote quoteInbound(Long erpTenantId, List<PurchaseInboundOrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        List<String> missing = new ArrayList<>();
        for (PurchaseInboundOrderItem item : items) {
            if (item.getActualQuantity() == null || item.getActualQuantity() <= 0) {
                continue;
            }
            SkuLookupVO sku = skuLookupMapper.findByTenantAndSku(erpTenantId, item.getSkuCode());
            BigDecimal unit = unitVolumeCbm(sku);
            if (unit == null) {
                missing.add(item.getSkuCode());
            }
            else {
                total = total.add(unit.multiply(BigDecimal.valueOf(item.getActualQuantity())));
            }
        }
        return new VolumeQuote(total.setScale(4, RoundingMode.HALF_UP), missing);
    }

    public BigDecimal recordInbound(PurchaseInboundOrder order, List<PurchaseInboundOrderItem> items,
            BigDecimal confirmedVolumeCbm, Boolean afterHours, String afterHoursReason) {
        Long operatorId = operatorId(order.getErpTenantId());
        VolumeQuote quote = quoteInbound(order.getErpTenantId(), items);
        BigDecimal volume = quote.getMissingSkuCodes().isEmpty() ? quote.getCalculatedVolumeCbm() : confirmedVolumeCbm;
        if (!quote.getMissingSkuCodes().isEmpty()) {
            Assert.isTrue(volume != null && volume.compareTo(BigDecimal.ZERO) > 0,
                    "SKU缺少有效包装尺寸，请填写本次确认入库体积(m³): "
                            + String.join("、", quote.getMissingSkuCodes()));
        }
        Assert.isTrue(volume != null && volume.compareTo(BigDecimal.ZERO) > 0, "确认入库体积必须大于0");

        BigDecimal base = postCharge("INBOUND:" + order.getId() + ":" + INBOUND_CBM,
                operatorId, order.getErpTenantId(), order.getWarehouseId(), "PURCHASE_INBOUND", order.getId(),
                order.getInboundNo(), INBOUND_CBM, volume, "入库上架完成");
        if (Boolean.TRUE.equals(afterHours)) {
            Assert.hasText(afterHoursReason, "加班附加费必须填写客户或服务商原因");
            WmsFeeRateCard rate = requireRate(operatorId, AFTER_HOURS_SURCHARGE);
            BigDecimal surcharge = base.multiply(rate.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            postFixedCharge("INBOUND:" + order.getId() + ":" + AFTER_HOURS_SURCHARGE,
                    operatorId, order.getErpTenantId(), order.getWarehouseId(), "PURCHASE_INBOUND", order.getId(),
                    order.getInboundNo(), rate, surcharge, afterHoursReason);
            return base.add(surcharge);
        }
        return base;
    }

    public OutboundHandlingPreviewVO previewOutbound(Long erpTenantId,
            List<WmsOutboundPickAllocation> allocations) {
        AllocationAnalysis analysis = analyzeAllocations(allocations);
        OutboundHandlingPreviewVO result = new OutboundHandlingPreviewVO();
        int wholeQty = 0;
        for (Map.Entry<Long, Integer> entry : analysis.wholePalletQuantity.entrySet()) {
            WmsPallet pallet = palletMapper.selectById(entry.getKey());
            OutboundHandlingPreviewVO.PalletCandidate item = new OutboundHandlingPreviewVO.PalletCandidate();
            item.setPalletId(entry.getKey());
            item.setPalletNo(pallet == null ? String.valueOf(entry.getKey()) : pallet.getPalletNo());
            item.setQuantity(entry.getValue());
            item.setSkuKinds(pallet == null ? null : pallet.getSkuKindCount());
            result.getWholePallets().add(item);
            wholeQty += entry.getValue();
        }
        result.setWholePalletQuantity(wholeQty);
        result.setLooseQuantity(Math.max(analysis.totalQuantity - wholeQty, 0));
        Long operatorId = operatorId(erpTenantId);
        BigDecimal fee = ratePrice(operatorId, OUTBOUND_FULL_PALLET)
                .multiply(BigDecimal.valueOf(result.getWholePallets().size()))
                .add(ratePrice(operatorId, OUTBOUND_SMALL_ITEM)
                        .multiply(BigDecimal.valueOf(result.getLooseQuantity())));
        result.setEstimatedFee(fee.setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    public BigDecimal recordOutbound(SalesOutboundOrder order, List<WmsOutboundPickAllocation> allocations,
            ShipDTO dto) {
        Long operatorId = operatorId(order.getErpTenantId());
        AllocationAnalysis analysis = analyzeAllocations(allocations);
        Set<Long> candidates = analysis.wholePalletQuantity.keySet();
        Set<Long> selected = dto.getFullPalletIds() == null
                ? new HashSet<>(candidates) : new HashSet<>(dto.getFullPalletIds());
        Assert.isTrue(candidates.containsAll(selected), "选择的整托不满足完整LPN离库条件，请刷新后重试");

        int wholeQty = selected.stream().mapToInt(id -> analysis.wholePalletQuantity.getOrDefault(id, 0)).sum();
        int looseQty = analysis.totalQuantity - wholeQty;
        int largeBoxes = value(dto.getLargeBoxCount());
        int smallItems = value(dto.getSmallItemCount());
        if (dto.getLargeBoxCount() == null && dto.getSmallItemCount() == null) {
            smallItems = looseQty;
        }
        Assert.isTrue(largeBoxes >= 0 && smallItems >= 0, "箱件和散件数量不能为负数");
        Assert.isTrue(largeBoxes + smallItems == looseQty,
                "非整托计费数量必须等于剩余出库数量，当前剩余" + looseQty + "件");

        BigDecimal total = BigDecimal.ZERO;
        if (!selected.isEmpty()) {
            total = total.add(postCharge("OUTBOUND:" + order.getId() + ":" + OUTBOUND_FULL_PALLET,
                    operatorId, order.getErpTenantId(), order.getWarehouseId(), "SALES_OUTBOUND", order.getId(),
                    order.getOutboundNo(), OUTBOUND_FULL_PALLET, BigDecimal.valueOf(selected.size()),
                    "完整离库托盘: " + selected.stream().sorted().map(String::valueOf).collect(Collectors.joining(","))));
        }
        if (largeBoxes > 0) {
            total = total.add(postCharge("OUTBOUND:" + order.getId() + ":" + OUTBOUND_LARGE_BOX,
                    operatorId, order.getErpTenantId(), order.getWarehouseId(), "SALES_OUTBOUND", order.getId(),
                    order.getOutboundNo(), OUTBOUND_LARGE_BOX, BigDecimal.valueOf(largeBoxes), "大箱按一箱一件计费"));
        }
        if (smallItems > 0) {
            total = total.add(postCharge("OUTBOUND:" + order.getId() + ":" + OUTBOUND_SMALL_ITEM,
                    operatorId, order.getErpTenantId(), order.getWarehouseId(), "SALES_OUTBOUND", order.getId(),
                    order.getOutboundNo(), OUTBOUND_SMALL_ITEM, BigDecimal.valueOf(smallItems), "小件散货按件计费"));
        }
        return total;
    }

    private AllocationAnalysis analyzeAllocations(List<WmsOutboundPickAllocation> allocations) {
        AllocationAnalysis result = new AllocationAnalysis();
        Map<Long, Integer> allocatedByPallet = new LinkedHashMap<>();
        for (WmsOutboundPickAllocation allocation : allocations) {
            int take = value(allocation.getTakeQty());
            result.totalQuantity += take;
            WmsPhysicalInventory batch = physicalInventoryMapper.selectById(allocation.getPhysicalInventoryId());
            if (batch != null && batch.getPalletId() != null) {
                allocatedByPallet.merge(batch.getPalletId(), take, Integer::sum);
            }
        }
        for (Map.Entry<Long, Integer> entry : allocatedByPallet.entrySet()) {
            int current = physicalInventoryMapper.listByPalletId(entry.getKey()).stream()
                    .mapToInt(batch -> value(batch.getQuantity())).sum();
            if (current > 0 && entry.getValue() == current) {
                result.wholePalletQuantity.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private BigDecimal postCharge(String bizId, Long operatorId, Long erpTenantId, Long warehouseId,
            String sourceType, Long sourceId, String sourceRef, String feeCode, BigDecimal quantity, String remark) {
        WmsBillingRecord existing = billingMapper.selectByBizId(bizId);
        if (existing != null) {
            return existing.getAmount();
        }
        WmsFeeRateCard rate = requireRate(operatorId, feeCode);
        BigDecimal amount = quantity.multiply(rate.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
        return insertRecord(bizId, operatorId, erpTenantId, warehouseId, sourceType, sourceId, sourceRef,
                rate, quantity, amount, remark);
    }

    private BigDecimal postFixedCharge(String bizId, Long operatorId, Long erpTenantId, Long warehouseId,
            String sourceType, Long sourceId, String sourceRef, WmsFeeRateCard rate, BigDecimal amount, String remark) {
        WmsBillingRecord existing = billingMapper.selectByBizId(bizId);
        if (existing != null) {
            return existing.getAmount();
        }
        return insertRecord(bizId, operatorId, erpTenantId, warehouseId, sourceType, sourceId, sourceRef,
                rate, BigDecimal.ONE, amount, remark);
    }

    private BigDecimal insertRecord(String bizId, Long operatorId, Long erpTenantId, Long warehouseId,
            String sourceType, Long sourceId, String sourceRef, WmsFeeRateCard rate, BigDecimal quantity,
            BigDecimal amount, String remark) {
        WmsBillingRecord record = new WmsBillingRecord();
        record.setBizId(bizId);
        record.setWmsTenantId(operatorId);
        record.setErpTenantId(erpTenantId);
        record.setWarehouseId(warehouseId);
        record.setBillMonth(YearMonth.now().toString());
        record.setFeeType(rate.getFeeType());
        record.setSourceType(sourceType);
        record.setSourceId(sourceId);
        record.setFeeCode(rate.getFeeCode());
        record.setBillingUnit(rate.getBillingUnit());
        record.setQuantity(quantity.setScale(0, RoundingMode.HALF_UP).intValue());
        record.setBillingQuantity(quantity);
        record.setUnitPrice(rate.getUnitPrice());
        record.setBaseAmount(amount);
        record.setAmount(amount);
        record.setCurrency(rate.getCurrency());
        record.setChargeStatus("POSTED");
        record.setRateSnapshot(rate.getFeeName() + "|" + rate.getBillingUnit() + "|" + rate.getUnitPrice());
        record.setSourceRef(sourceRef);
        record.setRemark(remark);
        billingMapper.insert(record);
        return amount;
    }

    private WmsFeeRateCard requireRate(Long operatorId, String feeCode) {
        WmsFeeRateCard rate = rateMapper.selectEffective(operatorId == null ? 0L : operatorId,
                feeCode, LocalDate.now());
        if (rate == null) {
            throw new BusinessException(400, "未配置有效仓储费率: " + feeCode);
        }
        return rate;
    }

    private BigDecimal ratePrice(Long operatorId, String feeCode) {
        return requireRate(operatorId, feeCode).getUnitPrice();
    }

    private Long operatorId(Long erpTenantId) {
        SysTenant owner = erpTenantId == null ? null : tenantMapper.selectById(erpTenantId);
        Assert.notNull(owner, "货主不存在");
        Assert.notNull(owner.getParentWmsTenantId(), "货主未关联WMS服务商，无法计费");
        return owner.getParentWmsTenantId();
    }

    private BigDecimal unitVolumeCbm(SkuLookupVO sku) {
        if (sku == null || nonPositive(sku.getPackageLength()) || nonPositive(sku.getPackageWidth())
                || nonPositive(sku.getPackageHeight())) {
            return null;
        }
        BigDecimal factor;
        if ("MM".equalsIgnoreCase(sku.getPackageUnit())) {
            factor = new BigDecimal("0.001");
        }
        else if ("CM".equalsIgnoreCase(sku.getPackageUnit())) {
            factor = new BigDecimal("0.01");
        }
        else if ("M".equalsIgnoreCase(sku.getPackageUnit())) {
            factor = BigDecimal.ONE;
        }
        else {
            return null;
        }
        return sku.getPackageLength().multiply(sku.getPackageWidth()).multiply(sku.getPackageHeight())
                .multiply(factor.pow(3)).setScale(9, RoundingMode.HALF_UP);
    }

    private boolean nonPositive(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    @lombok.Value
    public static class VolumeQuote {
        BigDecimal calculatedVolumeCbm;
        List<String> missingSkuCodes;
    }

    private static class AllocationAnalysis {
        private int totalQuantity;
        private final Map<Long, Integer> wholePalletQuantity = new LinkedHashMap<>();
    }
}
