package com.erp.admin.wms.facade;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.erp.admin.product.model.vo.SkuBriefVO;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.wms.calc.CalcBatch;
import com.erp.admin.wms.calc.ProductionResult;
import com.erp.admin.wms.calc.SalesMetrics;
import com.erp.admin.wms.calc.ShipProdCalcConstants;
import com.erp.admin.wms.calc.ShipProdCalcModel;
import com.erp.admin.wms.calc.ShippingResult;
import com.erp.admin.wms.calc.SkuCalcInput;
import com.erp.admin.wms.calc.SkuCalcResult;
import com.erp.admin.wms.mapper.ShipProdCalcMapper;
import com.erp.admin.wms.model.dto.PurchaseUnshippedBatchDTO;
import com.erp.admin.wms.model.dto.RegionSkuStockDTO;
import com.erp.admin.wms.model.dto.SkuDailySalesDTO;
import com.erp.admin.wms.model.entity.Region;
import com.erp.admin.wms.model.qo.ShipProdCalcQO;
import com.erp.admin.wms.model.vo.ShipProdCalcDetailVO;
import com.erp.admin.wms.model.vo.ShipProdCalcRowVO;
import com.erp.admin.wms.model.vo.ShipProdCalcSummaryVO;
import com.erp.admin.wms.service.RegionService;
import com.erp.admin.wms.service.RegionStockDataProvider;
import com.erp.admin.wms.service.ShippingOrderService;
import com.erp.admin.wms.service.FboInventorySnapshotService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 发货生产测算 Facade —— 组装 A/B/C/D+E + 逐日销量，逐 SKU 跑 {@link ShipProdCalcModel}。
 * <p>
 * 数据源（方案甲）：A+B=区域库存可用量、C=物流单在途批次、D+E=采购单未发货（当 E）、销量=FBS 逐日。
 * 全部经拦截器按当前货主自动隔离。
 *
 * @author erp
 */
@Component
@RequiredArgsConstructor
public class ShipProdCalcFacade {

    private static final int SALES_HISTORY_DAYS = 365;

    private final RegionService regionService;
    private final RegionStockDataProvider regionStockDataProvider;
    private final ShippingOrderService shippingOrderService;
    private final SkuBriefService skuBriefService;
    private final ShipProdCalcMapper shipProdCalcMapper;
    private final FboInventorySnapshotService fboInventorySnapshotService;

    // ============================================================ 汇总

    public ShipProdCalcSummaryVO getSummary(PageParam pageParam, ShipProdCalcQO qo) {
        LocalDate today = qo.getBaseDate() != null ? qo.getBaseDate() : LocalDate.now();

        List<Region> regions = regionService.listEnabled();
        Set<Long> regionIds = regions.stream().map(Region::getId).collect(Collectors.toSet());

        // A 现货可用量（各区域聚合到 SKU）
        Map<String, Integer> availableBySku = new HashMap<>();
        if (!regionIds.isEmpty()) {
            for (RegionSkuStockDTO s : regionStockDataProvider.getRegionSkuStocks(regionIds, null)) {
                availableBySku.merge(s.getSkuCode(), nz(s.getTotalAvailable()), Integer::sum);
            }
        }

        // B FBO 平台快照，与海外仓仓内可用量分开输入模型。
        Map<String, Integer> fboBySku = fboInventorySnapshotService
                .sumQuantityBySku(TenantContext.getCurrentTenant(), null);

        // E 采购未发货批次
        Map<String, List<CalcBatch>> eBySku = new HashMap<>();
        for (PurchaseUnshippedBatchDTO b : shipProdCalcMapper.selectPurchaseUnshippedBatches(null)) {
            eBySku.computeIfAbsent(b.getSkuCode(), k -> new ArrayList<>())
                    .add(new CalcBatch(nz(b.getQuantity()), b.getExpectedDeliveryDate(), b.getOrderNo()));
        }

        // 逐日销量（近 365 天，截止昨天）
        Map<String, Map<LocalDate, Integer>> salesBySku = loadDailySales(null, today);

        // SKU 全集 = 现货 ∪ 在制 ∪ 有销量
        Set<String> skus = new TreeSet<>();
        skus.addAll(availableBySku.keySet());
        skus.addAll(fboBySku.keySet());
        skus.addAll(eBySku.keySet());
        skus.addAll(salesBySku.keySet());

        // C 在途批次（物流单，按 SKU 聚合各区域）
        Map<String, List<CalcBatch>> cBySku = loadTransitBatches(regionIds, skus);

        // 逐 SKU 跑模型
        List<ShipProdCalcRowVO> rows = new ArrayList<>();
        for (String sku : skus) {
            SkuCalcInput input = SkuCalcInput.builder()
                    .sku(sku)
                    .overseas(availableBySku.getOrDefault(sku, 0))
                    .fbo(fboBySku.getOrDefault(sku, 0))
                    .transit(cBySku.getOrDefault(sku, Collections.emptyList()))
                    .factoryDone(0)
                    .producing(eBySku.getOrDefault(sku, Collections.emptyList()))
                    .build();
            SkuCalcResult r = ShipProdCalcModel.evaluateSku(input, salesBySku.getOrDefault(sku, Collections.emptyMap()), today);
            rows.add(toRow(input, r));
        }

        // 填充 SKU 简要信息
        if (!rows.isEmpty()) {
            skuBriefService.enrichForQuery(rows, ShipProdCalcRowVO::getSkuCode, ShipProdCalcRowVO::setSkuBrief);
        }

        // 关键字过滤（编码/名称）
        List<ShipProdCalcRowVO> keywordFiltered = filterByKeyword(rows, qo.getSkuKeyword());

        // 状态卡计数（基于关键字过滤后、决策过滤前）
        ShipProdCalcSummaryVO.Counts counts = countStatuses(keywordFiltered);

        // 决策过滤
        List<ShipProdCalcRowVO> filtered = filterByDecision(keywordFiltered, qo.getDecision());

        // 排序：全链路支撑天数升序（最紧急在前，∞/null 最后）
        filtered.sort((x, y) -> compareNullableAsc(x.getTotalSupportDays(), y.getTotalSupportDays()));

        long total = filtered.size();
        List<ShipProdCalcRowVO> paged = paginate(filtered, pageParam);

        return ShipProdCalcSummaryVO.builder()
                .total(total)
                .counts(counts)
                .shipThresholdDays(ShipProdCalcConstants.SHIP_THRESHOLD_DAYS)
                .prodThresholdDays(ShipProdCalcConstants.PROD_THRESHOLD_DAYS)
                .safetyStockDays(ShipProdCalcConstants.SAFETY_STOCK_DAYS)
                .list(paged)
                .build();
    }

    // ============================================================ 详情

    public ShipProdCalcDetailVO getDetail(String skuCode, LocalDate baseDate) {
        LocalDate today = baseDate != null ? baseDate : LocalDate.now();

        List<Region> regions = regionService.listEnabled();
        Set<Long> regionIds = regions.stream().map(Region::getId).collect(Collectors.toSet());
        Set<String> one = Collections.singleton(skuCode);

        // A 现货
        int available = 0;
        if (!regionIds.isEmpty()) {
            for (RegionSkuStockDTO s : regionStockDataProvider.getRegionSkuStocks(regionIds, skuCode)) {
                if (skuCode.equals(s.getSkuCode())) {
                    available += nz(s.getTotalAvailable());
                }
            }
        }
        int fboQuantity = fboInventorySnapshotService
                .sumQuantityBySku(TenantContext.getCurrentTenant(), one)
                .getOrDefault(skuCode, 0);
        // C 在途
        List<CalcBatch> cBatches = loadTransitBatches(regionIds, one).getOrDefault(skuCode, Collections.emptyList());
        // E 在制
        List<CalcBatch> eBatches = new ArrayList<>();
        for (PurchaseUnshippedBatchDTO b : shipProdCalcMapper.selectPurchaseUnshippedBatches(one)) {
            eBatches.add(new CalcBatch(nz(b.getQuantity()), b.getExpectedDeliveryDate(), b.getOrderNo()));
        }
        // 销量
        Map<LocalDate, Integer> daily = loadDailySales(one, today).getOrDefault(skuCode, Collections.emptyMap());

        SkuCalcInput input = SkuCalcInput.builder()
                .sku(skuCode).overseas(available).fbo(fboQuantity)
                .transit(cBatches).factoryDone(0).producing(eBatches)
                .build();
        SkuCalcResult r = ShipProdCalcModel.evaluateSku(input, daily, today);
        SalesMetrics m = r.getSales();
        ShippingResult sh = r.getShipping();
        ProductionResult pr = r.getProduction();

        Map<String, SkuBriefVO> briefMap = skuBriefService.buildMapForQuery(one);

        return ShipProdCalcDetailVO.builder()
                .skuCode(skuCode)
                .skuBrief(briefMap.get(skuCode))
                .baseDate(today)
                .overseas(available).fbo(fboQuantity)
                .inTransit(totalQty(cBatches)).factoryDone(0).producing(totalQty(eBatches))
                .xt(round1(m.getXt())).yt(round1(m.getYt())).zt(round1(m.getZt()))
                .zt1(round1(m.getZt1())).zt2(round1(m.getZt2())).zt3(round1(m.getZt3()))
                .avg7(round1(m.getAvg7())).avg15(round1(m.getAvg15())).avg30(round1(m.getAvg30()))
                .kBase(round2(m.getKBase())).peakSamples(m.getPeakSamples()).historyDays(m.getHistoryDays())
                .shipSupportDays(finiteOrNull(sh.getS())).shipDtC(sh.getDtC())
                .totalSupportDays(finiteOrNull(sh.getSTotal()))
                .needShip(sh.isNeedShip()).shipPlanQty(sh.getPlanQty()).shipPath(sh.getPath())
                .prodSupportDays(finiteOrNull(pr.getSProd())).prodDtE(pr.getDtE())
                .prodTotalSupportDays(finiteOrNull(pr.getSProdTotal()))
                .needProduce(pr.isNeedProduce()).prodPlanQty(pr.getPlanQty()).prodPath(pr.getPath())
                .earliestArrivalDate(plusDaysOrNull(today, sh.getDtC()))
                .earliestCompletionDate(plusDaysOrNull(today, pr.getDtE()))
                .stockoutDate(Double.isFinite(sh.getSTotal()) ? today.plusDays((long) Math.floor(sh.getSTotal())) : null)
                .shipRedLineDate(today.plusDays(ShipProdCalcConstants.SHIP_THRESHOLD_DAYS))
                .prodRedLineDate(today.plusDays(ShipProdCalcConstants.PROD_THRESHOLD_DAYS))
                .transitBatches(toBatchVOs(cBatches))
                .producingBatches(toBatchVOs(eBatches))
                .warnings(r.getWarnings())
                .build();
    }

    // ============================================================ 私有

    /** 逐日销量：Map&lt;sku, Map&lt;date, qty&gt;&gt;，截止昨天、近 365 天。 */
    private Map<String, Map<LocalDate, Integer>> loadDailySales(Set<String> skuCodes, LocalDate today) {
        LocalDateTime start = today.minusDays(SALES_HISTORY_DAYS).atStartOfDay();
        LocalDateTime end = today.atStartOfDay();
        Map<String, Map<LocalDate, Integer>> salesBySku = new HashMap<>();
        for (SkuDailySalesDTO d : shipProdCalcMapper.selectFbsDailySales(skuCodes, start, end)) {
            salesBySku.computeIfAbsent(d.getSkuCode(), k -> new HashMap<>())
                    .merge(d.getSaleDate(), nz(d.getQuantity()), Integer::sum);
        }
        return salesBySku;
    }

    /** 在途批次：物流单已发未收，按 SKU 聚合各区域（仅含有 ETA 的批次）。 */
    private Map<String, List<CalcBatch>> loadTransitBatches(Set<Long> regionIds, Set<String> skus) {
        Map<String, List<CalcBatch>> cBySku = new HashMap<>();
        if (regionIds.isEmpty() || skus.isEmpty()) {
            return cBySku;
        }
        Map<String, Map<LocalDate, Integer>> incoming = shippingOrderService.batchGetIncomingByDate(regionIds, skus);
        // 先按 SKU 汇总日期→数量，再转批次
        Map<String, Map<LocalDate, Integer>> bySku = new HashMap<>();
        for (Map.Entry<String, Map<LocalDate, Integer>> e : incoming.entrySet()) {
            String key = e.getKey();
            String sku = key.substring(key.indexOf(':') + 1);
            Map<LocalDate, Integer> agg = bySku.computeIfAbsent(sku, k -> new HashMap<>());
            e.getValue().forEach((date, qty) -> agg.merge(date, qty, Integer::sum));
        }
        bySku.forEach((sku, dateMap) -> {
            List<CalcBatch> batches = dateMap.entrySet().stream()
                    .map(en -> new CalcBatch(en.getValue(), en.getKey(), "在途"))
                    .collect(Collectors.toList());
            cBySku.put(sku, batches);
        });
        return cBySku;
    }

    private ShipProdCalcRowVO toRow(SkuCalcInput input, SkuCalcResult r) {
        SalesMetrics m = r.getSales();
        ShippingResult sh = r.getShipping();
        ProductionResult pr = r.getProduction();
        boolean shortage = sh.getDtC() != null && sh.getS() < sh.getDtC();
        return ShipProdCalcRowVO.builder()
                .skuCode(input.getSku())
                .onHand(input.getOverseas() + input.getFbo())
                .inTransit(totalQty(input.getTransit()))
                .producing(totalQty(input.getProducing()))
                .xt(round1(m.getXt())).yt(round1(m.getYt())).zt(round1(m.getZt()))
                .shipSupportDays(finiteOrNull(sh.getS()))
                .totalSupportDays(finiteOrNull(sh.getSTotal()))
                .prodSupportDays(finiteOrNull(pr.getSProdTotal()))
                .needShip(sh.isNeedShip()).shipPlanQty(sh.getPlanQty()).shipPath(sh.getPath())
                .needProduce(pr.isNeedProduce()).prodPlanQty(pr.getPlanQty()).prodPath(pr.getPath())
                .shortage(shortage)
                .noSales(m.getZt1() <= 0)
                .warnings(r.getWarnings())
                .build();
    }

    private List<ShipProdCalcRowVO> filterByKeyword(List<ShipProdCalcRowVO> rows, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return rows;
        }
        String kw = keyword.trim().toLowerCase();
        return rows.stream().filter(row -> {
            if (row.getSkuCode() != null && row.getSkuCode().toLowerCase().contains(kw)) {
                return true;
            }
            SkuBriefVO b = row.getSkuBrief();
            return b != null && b.getSkuName() != null && b.getSkuName().toLowerCase().contains(kw);
        }).collect(Collectors.toList());
    }

    private List<ShipProdCalcRowVO> filterByDecision(List<ShipProdCalcRowVO> rows, String decision) {
        if (!StringUtils.hasText(decision)) {
            return new ArrayList<>(rows);
        }
        return rows.stream().filter(row -> {
            switch (decision) {
                case "SHIP": return row.isNeedShip();
                case "PRODUCE": return row.isNeedProduce();
                case "SHORTAGE": return row.isShortage();
                case "NO_SALES": return row.isNoSales();
                default: return true;
            }
        }).collect(Collectors.toList());
    }

    private ShipProdCalcSummaryVO.Counts countStatuses(List<ShipProdCalcRowVO> rows) {
        int ship = 0, produce = 0, shortage = 0, noSales = 0;
        for (ShipProdCalcRowVO row : rows) {
            if (row.isNeedShip()) ship++;
            if (row.isNeedProduce()) produce++;
            if (row.isShortage()) shortage++;
            if (row.isNoSales()) noSales++;
        }
        return ShipProdCalcSummaryVO.Counts.builder()
                .needShip(ship).needProduce(produce).shortage(shortage).noSales(noSales).build();
    }

    private List<ShipProdCalcRowVO> paginate(List<ShipProdCalcRowVO> rows, PageParam pageParam) {
        int total = rows.size();
        int start = (int) ((pageParam.getPage() - 1) * pageParam.getSize());
        if (start >= total || start < 0) {
            return Collections.emptyList();
        }
        int end = Math.min(start + (int) pageParam.getSize(), total);
        return new ArrayList<>(rows.subList(start, end));
    }

    private List<ShipProdCalcDetailVO.BatchVO> toBatchVOs(List<CalcBatch> batches) {
        return batches.stream()
                .map(b -> ShipProdCalcDetailVO.BatchVO.builder()
                        .qty(b.getQty()).eta(b.getEta()).label(b.getLabel()).build())
                .collect(Collectors.toList());
    }

    private static int totalQty(List<CalcBatch> batches) {
        return batches == null ? 0 : batches.stream().mapToInt(CalcBatch::getQty).sum();
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** 有限值四舍五入到 1 位；∞ 返回 null（前端显示 ∞）。 */
    private static Double finiteOrNull(double v) {
        return Double.isFinite(v) ? round1(v) : null;
    }

    private static LocalDate plusDaysOrNull(LocalDate base, Double days) {
        return days == null ? null : base.plusDays(Math.round(days));
    }

    /** 升序比较，null 视为最大（排最后）。 */
    private static int compareNullableAsc(Double a, Double b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return Double.compare(a, b);
    }
}
