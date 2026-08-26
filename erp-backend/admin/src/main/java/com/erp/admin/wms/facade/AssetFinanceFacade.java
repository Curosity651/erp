package com.erp.admin.wms.facade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.mapper.AssetFinanceMapper;
import com.erp.admin.wms.mapper.RegionInventoryMapper;
import com.erp.admin.wms.model.dto.AssetLocationStockDTO;
import com.erp.admin.wms.model.dto.PayableProviderRowDTO;
import com.erp.admin.wms.model.dto.PayableSupplierRowDTO;
import com.erp.admin.wms.model.dto.PurchaseCostAggDTO;
import com.erp.admin.wms.model.dto.PurchaseUnshippedBatchDTO;
import com.erp.admin.wms.model.dto.ShippingCostLineDTO;
import com.erp.admin.wms.model.dto.SkuQuantityDTO;
import com.erp.admin.wms.model.entity.Region;
import com.erp.admin.wms.model.vo.AssetFinanceOverviewVO;
import com.erp.admin.wms.model.vo.AssetLogisticsRowVO;
import com.erp.admin.wms.model.vo.AssetOverviewVO;
import com.erp.admin.wms.model.vo.AssetProcurementRowVO;
import com.erp.admin.wms.model.vo.FboInventorySummaryVO;
import com.erp.admin.wms.model.vo.PayableProviderVO;
import com.erp.admin.wms.model.vo.PayablesOverviewVO;
import com.erp.admin.wms.model.vo.PayableSupplierVO;
import com.erp.admin.wms.service.RegionService;
import com.erp.admin.wms.service.FboInventorySnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 货主资产与账务 Facade（加权平均估值、分币种不折算）。
 * <p>
 * 资产 = 采购成本(A–E 持有 × 加权采购单价) + 物流附加(A+B+C 已发运 × 物流单位成本，USD)。
 * 账务 = 应付供应商(采购单预付/尾款) + 应付物流商(物流单)。全部纯聚合，零建表。
 *
 * @author erp
 */
@Component
@RequiredArgsConstructor
public class AssetFinanceFacade {

    private static final String USD = "USD";

    private final RegionService regionService;
    private final RegionInventoryMapper regionInventoryMapper;
    private final AssetFinanceMapper assetFinanceMapper;
    private final SkuBriefService skuBriefService;
    private final FboInventorySnapshotService fboInventorySnapshotService;

    // ============================================================ 总览

    public AssetFinanceOverviewVO getOverview() {
        AssetOverviewVO asset = getAssetsOverview();
        PayablesOverviewVO payable = getPayablesOverview();
        return AssetFinanceOverviewVO.builder()
                .assets(asset.getAssets())
                .supplierPayable(payable.getSupplierPayable())
                .providerPayable(payable.getProviderPayable())
                .unvaluedSkuCount(asset.getUnvaluedSkuCount())
                .unvaluedQuantity(asset.getUnvaluedQuantity())
                .totalHeldQuantity(asset.getTotalHeldQuantity())
                .holdingPositions(asset.getHoldingPositions())
                .fboLastSyncedAt(asset.getFboLastSyncedAt())
                .fboStale(asset.getFboStale())
                .build();
    }

    public AssetOverviewVO getAssetsOverview() {
        Long tenantId = requireOwnerTenant();
        HeldQty held = loadHeldQty();
        List<AssetProcurementRowVO> proc = computeProcurementRows(held);
        List<AssetLogisticsRowVO> logi = computeLogisticsRows(held);

        Set<String> valuedSkus = new TreeSet<>();
        for (PurchaseCostAggDTO row : assetFinanceMapper.selectPurchaseCostAgg(tenantId)) {
            if (nz(row.getQty()) > 0) valuedSkus.add(row.getSkuCode());
        }
        int unvaluedQuantity = held.universe.stream()
                .filter(sku -> !valuedSkus.contains(sku))
                .mapToInt(held::total).sum();
        int unvaluedSkuCount = (int) held.universe.stream()
                .filter(sku -> held.total(sku) > 0 && !valuedSkus.contains(sku)).count();

        // 采购成本按币种
        Map<String, BigDecimal> procByCur = new LinkedHashMap<>();
        for (AssetProcurementRowVO r : proc) {
            procByCur.merge(r.getCurrency(), r.getProcurementCost(), BigDecimal::add);
        }
        // 物流附加合计（USD）
        BigDecimal logiTotal = BigDecimal.ZERO;
        for (AssetLogisticsRowVO r : logi) {
            logiTotal = logiTotal.add(r.getLogisticsCostUsd());
        }

        // 资产分币种行
        Set<String> currencies = new TreeSet<>(procByCur.keySet());
        if (logiTotal.signum() > 0) {
            currencies.add(USD);
        }
        List<AssetFinanceOverviewVO.AssetCurrencyVO> assets = new ArrayList<>();
        for (String cur : currencies) {
            BigDecimal p = procByCur.getOrDefault(cur, BigDecimal.ZERO);
            BigDecimal l = USD.equals(cur) ? logiTotal : BigDecimal.ZERO;
            assets.add(AssetFinanceOverviewVO.AssetCurrencyVO.builder()
                    .currency(cur).procurement(money(p)).logistics(money(l)).total(money(p.add(l)))
                    .build());
        }

        FboInventorySummaryVO fboSummary = fboInventorySnapshotService.getSummary();
        return AssetOverviewVO.builder()
                .assets(assets)
                .unvaluedSkuCount(unvaluedSkuCount)
                .unvaluedQuantity(unvaluedQuantity)
                .totalHeldQuantity(held.total())
                .holdingPositions(buildHoldingPositions(held))
                .fboLastSyncedAt(fboSummary.getLastSyncedAt())
                .fboStale(fboSummary.getStale())
                .build();
    }

    public PayablesOverviewVO getPayablesOverview() {
        requireOwnerTenant();
        // 应付供应商按币种
        Map<String, BigDecimal[]> supByCur = new LinkedHashMap<>();
        for (PayableSupplierVO s : computePayableSuppliers()) {
            supByCur.computeIfAbsent(s.getCurrency(), k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
            BigDecimal[] acc = supByCur.get(s.getCurrency());
            acc[0] = acc[0].add(s.getContract());
            acc[1] = acc[1].add(s.getPaid());
            acc[2] = acc[2].add(s.getOutstanding());
        }
        List<AssetFinanceOverviewVO.PayableCurrencyVO> supplierPayable = new ArrayList<>();
        supByCur.forEach((cur, acc) -> supplierPayable.add(AssetFinanceOverviewVO.PayableCurrencyVO.builder()
                .currency(cur).contract(money(acc[0])).paid(money(acc[1])).outstanding(money(acc[2])).build()));

        // 应付物流商（USD 合计）
        BigDecimal pc = BigDecimal.ZERO, pp = BigDecimal.ZERO, po = BigDecimal.ZERO;
        for (PayableProviderVO v : computePayableProviders()) {
            pc = pc.add(v.getContract());
            pp = pp.add(v.getPaid());
            po = po.add(v.getOutstanding());
        }
        AssetFinanceOverviewVO.PayableCurrencyVO providerPayable = AssetFinanceOverviewVO.PayableCurrencyVO.builder()
                .currency(USD).contract(money(pc)).paid(money(pp)).outstanding(money(po)).build();

        return PayablesOverviewVO.builder()
                .supplierPayable(supplierPayable)
                .providerPayable(providerPayable)
                .build();
    }

    // ============================================================ 明细

    public List<AssetProcurementRowVO> getProcurementDetail() {
        List<AssetProcurementRowVO> rows = computeProcurementRows(loadHeldQty());
        if (!rows.isEmpty()) {
            skuBriefService.enrichForQuery(rows, AssetProcurementRowVO::getSkuCode, AssetProcurementRowVO::setSkuBrief);
        }
        return rows;
    }

    public List<AssetLogisticsRowVO> getLogisticsDetail() {
        List<AssetLogisticsRowVO> rows = computeLogisticsRows(loadHeldQty());
        if (!rows.isEmpty()) {
            skuBriefService.enrichForQuery(rows, AssetLogisticsRowVO::getSkuCode, AssetLogisticsRowVO::setSkuBrief);
        }
        return rows;
    }

    public List<PayableSupplierVO> getPayableSupplier() {
        return computePayableSuppliers();
    }

    public List<PayableProviderVO> getPayableProvider() {
        return computePayableProviders();
    }

    // ============================================================ 计算

    /** 持有量：A+B 现货 / C 在途 / D+E 在制（采购未发货）。 */
    private HeldQty loadHeldQty() {
        HeldQty h = new HeldQty();
        Long tenantId = requireOwnerTenant();
        for (AssetLocationStockDTO stock : assetFinanceMapper.selectLocationHeldQuantity(tenantId)) {
            h.available.merge(stock.getSkuCode(), nz(stock.getAvailableQuantity()), Integer::sum);
            h.reserved.merge(stock.getSkuCode(), nz(stock.getReservedQuantity()), Integer::sum);
            h.damaged.merge(stock.getSkuCode(), nz(stock.getDamagedQuantity()), Integer::sum);
        }
        List<Region> regions = regionService.listEnabled();
        Set<Long> regionIds = new TreeSet<>();
        regions.forEach(r -> regionIds.add(r.getId()));
        if (!regionIds.isEmpty()) {
            for (SkuQuantityDTO row : regionInventoryMapper.selectInTransitQuantityBySku(tenantId, regionIds)) {
                h.inTransit.merge(row.getSkuCode(), nz(row.getQuantity()), Integer::sum);
            }
        }
        for (PurchaseUnshippedBatchDTO b : assetFinanceMapper.selectPurchaseUnshippedBatches(tenantId)) {
            h.unshipped.merge(b.getSkuCode(), nz(b.getQuantity()), Integer::sum);
        }
        Map<String, Integer> fbo = fboInventorySnapshotService.sumQuantityBySku(
                tenantId, null);
        h.fbo.putAll(fbo);
        h.universe.addAll(h.available.keySet());
        h.universe.addAll(h.reserved.keySet());
        h.universe.addAll(h.inTransit.keySet());
        h.universe.addAll(h.damaged.keySet());
        h.universe.addAll(h.unshipped.keySet());
        h.universe.addAll(h.fbo.keySet());
        return h;
    }

    private List<AssetProcurementRowVO> computeProcurementRows(HeldQty held) {
        // 采购聚合：sku → currency → (qty, amount)
        Map<String, Map<String, PurchaseCostAggDTO>> bySku = new HashMap<>();
        Map<String, Integer> totalPurchased = new HashMap<>();
        for (PurchaseCostAggDTO a : assetFinanceMapper.selectPurchaseCostAgg(requireOwnerTenant())) {
            bySku.computeIfAbsent(a.getSkuCode(), k -> new LinkedHashMap<>()).put(a.getCurrency(), a);
            totalPurchased.merge(a.getSkuCode(), nz(a.getQty()), Integer::sum);
        }

        List<AssetProcurementRowVO> rows = new ArrayList<>();
        for (String sku : held.universe) {
            int total = held.total(sku);
            if (total <= 0) {
                continue;
            }
            Map<String, PurchaseCostAggDTO> curMap = bySku.get(sku);
            int purchasedTotal = totalPurchased.getOrDefault(sku, 0);
            if (curMap == null || purchasedTotal <= 0) {
                continue; // 无采购价，无法估值
            }
            List<Map.Entry<String, PurchaseCostAggDTO>> entries = new ArrayList<>(curMap.entrySet());
            entries.sort(Map.Entry.comparingByKey());
            Map<String, Integer> allocation = new HashMap<>();
            List<CurrencyRemainder> remainders = new ArrayList<>();
            int allocated = 0;
            for (Map.Entry<String, PurchaseCostAggDTO> e : entries) {
                long numerator = (long) total * nz(e.getValue().getQty());
                int qty = (int) (numerator / purchasedTotal);
                allocation.put(e.getKey(), qty);
                allocated += qty;
                remainders.add(new CurrencyRemainder(e.getKey(), numerator % purchasedTotal));
            }
            remainders.sort(Comparator.comparingLong(CurrencyRemainder::getRemainder).reversed()
                    .thenComparing(CurrencyRemainder::getCurrency));
            for (int i = 0; i < total - allocated; i++) {
                String currency = remainders.get(i % remainders.size()).getCurrency();
                allocation.merge(currency, 1, Integer::sum);
            }
            for (Map.Entry<String, PurchaseCostAggDTO> e : entries) {
                PurchaseCostAggDTO agg = e.getValue();
                int aggQty = nz(agg.getQty());
                if (aggQty <= 0) {
                    continue;
                }
                int heldInCur = allocation.getOrDefault(e.getKey(), 0);
                if (heldInCur <= 0) {
                    continue;
                }
                BigDecimal avgPrice = nz(agg.getAmount()).divide(BigDecimal.valueOf(aggQty), 4, RoundingMode.HALF_UP);
                BigDecimal cost = avgPrice.multiply(BigDecimal.valueOf(heldInCur));
                rows.add(AssetProcurementRowVO.builder()
                        .skuCode(sku).currency(e.getKey()).heldQty(heldInCur)
                        .avgUnitPrice(avgPrice).procurementCost(money(cost))
                        .build());
            }
        }
        return rows;
    }

    private List<AssetLogisticsRowVO> computeLogisticsRows(HeldQty held) {
        List<ShippingCostLineDTO> lines = assetFinanceMapper.selectShippingCostLines(requireOwnerTenant());
        // 每物流单：总额 USD + 总数量（分摊基数）
        Map<Long, BigDecimal> orderTotal = new HashMap<>();
        Map<Long, Integer> orderQty = new HashMap<>();
        for (ShippingCostLineDTO l : lines) {
            orderTotal.putIfAbsent(l.getShippingOrderId(), nz(l.getOrderTotalUsd()));
            orderQty.merge(l.getShippingOrderId(), nz(l.getQuantity()), Integer::sum);
        }
        // 每 SKU：分摊物流费 + 发运量
        Map<String, BigDecimal> feeBySku = new HashMap<>();
        Map<String, Integer> qtyBySku = new HashMap<>();
        for (ShippingCostLineDTO l : lines) {
            int oQty = orderQty.getOrDefault(l.getShippingOrderId(), 0);
            if (oQty <= 0) {
                continue;
            }
            BigDecimal oTotal = orderTotal.getOrDefault(l.getShippingOrderId(), BigDecimal.ZERO);
            BigDecimal allocated = oTotal.multiply(BigDecimal.valueOf(nz(l.getQuantity())))
                    .divide(BigDecimal.valueOf(oQty), 4, RoundingMode.HALF_UP);
            feeBySku.merge(l.getSkuCode(), allocated, BigDecimal::add);
            qtyBySku.merge(l.getSkuCode(), nz(l.getQuantity()), Integer::sum);
        }

        List<AssetLogisticsRowVO> rows = new ArrayList<>();
        for (String sku : held.universe) {
            int shippedHeld = held.shipped(sku);
            if (shippedHeld <= 0) {
                continue;
            }
            int shippedQty = qtyBySku.getOrDefault(sku, 0);
            if (shippedQty <= 0) {
                continue; // 无物流成本数据
            }
            BigDecimal unitCost = feeBySku.getOrDefault(sku, BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(shippedQty), 4, RoundingMode.HALF_UP);
            int capitalizedQty = Math.min(shippedHeld, shippedQty);
            BigDecimal cost = unitCost.multiply(BigDecimal.valueOf(capitalizedQty));
            rows.add(AssetLogisticsRowVO.builder()
                    .skuCode(sku).shippedHeldQty(capitalizedQty)
                    .unitCostUsd(unitCost).logisticsCostUsd(money(cost))
                    .build());
        }
        return rows;
    }

    private List<PayableSupplierVO> computePayableSuppliers() {
        List<PayableSupplierRowDTO> rows = assetFinanceMapper.selectPayableSupplierRows(requireOwnerTenant());
        Map<String, PayableSupplierVO> group = new LinkedHashMap<>();
        Map<String, List<PayableSupplierVO.OrderVO>> orders = new HashMap<>();
        Map<String, BigDecimal[]> acc = new HashMap<>();

        for (PayableSupplierRowDTO r : rows) {
            BigDecimal total = nz(r.getTotalAmount());
            BigDecimal prepay = nz(r.getPrepayAmount());
            BigDecimal balance = total.subtract(prepay);
            boolean prepayPaid = intEq(r.getPrepayStatus(), 1);
            boolean balancePaid = intEq(r.getBalanceStatus(), 1);
            BigDecimal paid = (prepayPaid ? prepay : BigDecimal.ZERO).add(balancePaid ? balance : BigDecimal.ZERO);
            BigDecimal outstanding = total.subtract(paid);

            String key = r.getSupplierId() + "|" + safe(r.getCurrency());
            group.computeIfAbsent(key, k -> PayableSupplierVO.builder()
                    .supplierId(r.getSupplierId()).supplierName(r.getSupplierName()).currency(r.getCurrency())
                    .contract(BigDecimal.ZERO).paid(BigDecimal.ZERO).outstanding(BigDecimal.ZERO).build());
            acc.computeIfAbsent(key, k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
            BigDecimal[] a = acc.get(key);
            a[0] = a[0].add(total);
            a[1] = a[1].add(paid);
            a[2] = a[2].add(outstanding);

            orders.computeIfAbsent(key, k -> new ArrayList<>()).add(PayableSupplierVO.OrderVO.builder()
                    .orderNo(r.getOrderNo()).orderStatus(r.getOrderStatus())
                    .totalAmount(money(total)).prepayAmount(money(prepay)).balanceAmount(money(balance))
                    .prepayPaid(prepayPaid).balancePaid(balancePaid)
                    .paid(money(paid)).outstanding(money(outstanding))
                    .prepayTime(r.getPrepayTime()).balancePayTime(r.getBalancePayTime())
                    .build());
        }

        List<PayableSupplierVO> result = new ArrayList<>();
        for (Map.Entry<String, PayableSupplierVO> e : group.entrySet()) {
            PayableSupplierVO v = e.getValue();
            BigDecimal[] a = acc.get(e.getKey());
            v.setContract(money(a[0]));
            v.setPaid(money(a[1]));
            v.setOutstanding(money(a[2]));
            v.setOrders(orders.get(e.getKey()));
            result.add(v);
        }
        return result;
    }

    private List<PayableProviderVO> computePayableProviders() {
        List<PayableProviderRowDTO> rows = assetFinanceMapper.selectPayableProviderRows(requireOwnerTenant());
        Map<Long, PayableProviderVO> group = new LinkedHashMap<>();
        Map<Long, List<PayableProviderVO.OrderVO>> orders = new HashMap<>();
        Map<Long, BigDecimal[]> acc = new HashMap<>();

        for (PayableProviderRowDTO r : rows) {
            BigDecimal total = nz(r.getTotalAmount());
            BigDecimal cny = nz(r.getTotalAmountCny());
            boolean paidFlag = intEq(r.getPaymentStatus(), 1);
            BigDecimal paid = paidFlag ? total : BigDecimal.ZERO;
            BigDecimal outstanding = total.subtract(paid);

            Long key = r.getProviderId();
            group.computeIfAbsent(key, k -> PayableProviderVO.builder()
                    .providerId(r.getProviderId()).providerName(r.getProviderName())
                    .contract(BigDecimal.ZERO).contractCny(BigDecimal.ZERO)
                    .paid(BigDecimal.ZERO).outstanding(BigDecimal.ZERO).build());
            acc.computeIfAbsent(key, k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
            BigDecimal[] a = acc.get(key);
            a[0] = a[0].add(total);
            a[1] = a[1].add(cny);
            a[2] = a[2].add(paid);
            a[3] = a[3].add(outstanding);

            orders.computeIfAbsent(key, k -> new ArrayList<>()).add(PayableProviderVO.OrderVO.builder()
                    .shippingNo(r.getShippingNo()).shippingStatus(r.getShippingStatus())
                    .totalAmount(money(total)).totalAmountCny(money(cny))
                    .paid(paidFlag).outstanding(money(outstanding))
                    .build());
        }

        List<PayableProviderVO> result = new ArrayList<>();
        for (Map.Entry<Long, PayableProviderVO> e : group.entrySet()) {
            PayableProviderVO v = e.getValue();
            BigDecimal[] a = acc.get(e.getKey());
            v.setContract(money(a[0]));
            v.setContractCny(money(a[1]));
            v.setPaid(money(a[2]));
            v.setOutstanding(money(a[3]));
            v.setOrders(orders.get(e.getKey()));
            result.add(v);
        }
        return result;
    }

    // ============================================================ 工具

    /** 持有量持有器：A+B / C / D+E。 */
    private static class HeldQty {
        final Map<String, Integer> available = new HashMap<>();
        final Map<String, Integer> reserved = new HashMap<>();
        final Map<String, Integer> inTransit = new HashMap<>();
        final Map<String, Integer> damaged = new HashMap<>();
        final Map<String, Integer> unshipped = new HashMap<>();
        final Map<String, Integer> fbo = new HashMap<>();
        final Set<String> universe = new TreeSet<>();

        int total(String sku) {
            return available.getOrDefault(sku, 0) + reserved.getOrDefault(sku, 0)
                    + inTransit.getOrDefault(sku, 0) + damaged.getOrDefault(sku, 0)
                    + unshipped.getOrDefault(sku, 0) + fbo.getOrDefault(sku, 0);
        }

        int shipped(String sku) {
            return available.getOrDefault(sku, 0) + reserved.getOrDefault(sku, 0)
                    + inTransit.getOrDefault(sku, 0) + damaged.getOrDefault(sku, 0)
                    + fbo.getOrDefault(sku, 0);
        }

        int sum(Map<String, Integer> values) { return values.values().stream().mapToInt(Integer::intValue).sum(); }
        int total() { return universe.stream().mapToInt(this::total).sum(); }
    }

    private List<AssetFinanceOverviewVO.HoldingPositionVO> buildHoldingPositions(HeldQty held) {
        List<AssetFinanceOverviewVO.HoldingPositionVO> rows = new ArrayList<>();
        rows.add(position("UNSHIPPED", "采购未发货", held.sum(held.unshipped)));
        rows.add(position("IN_TRANSIT", "国内至海外仓在途", held.sum(held.inTransit)));
        rows.add(position("OWN_AVAILABLE", "海外仓可用", held.sum(held.available)));
        rows.add(position("OWN_RESERVED", "海外仓预占", held.sum(held.reserved)));
        rows.add(position("OWN_DAMAGED", "海外仓残品", held.sum(held.damaged)));
        rows.add(position("FBO_TRANSIT", "FBO在途（暂未跟踪）", 0));
        rows.add(position("FBO", "FBO平台库存", held.sum(held.fbo)));
        return rows;
    }

    private AssetFinanceOverviewVO.HoldingPositionVO position(String code, String name, int quantity) {
        return AssetFinanceOverviewVO.HoldingPositionVO.builder().code(code).name(name).quantity(quantity).build();
    }

    private static class CurrencyRemainder {
        private final String currency;
        private final long remainder;
        CurrencyRemainder(String currency, long remainder) { this.currency = currency; this.remainder = remainder; }
        String getCurrency() { return currency; }
        long getRemainder() { return remainder; }
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static boolean intEq(Integer v, int x) {
        return v != null && v == x;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    private Long requireOwnerTenant() {
        Long tenantId = com.erp.admin.common.tenant.TenantContext.getCurrentTenant();
        if (tenantId == null || com.erp.admin.common.tenant.TenantContext.BLOCK_TENANT_ID.equals(tenantId)) {
            throw new IllegalArgumentException("资产与账务仅支持 ERP 货主访问");
        }
        return tenantId;
    }
}
