package com.erp.admin.wms.calc;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.erp.admin.wms.calc.ShipProdCalcConstants.INF_DAYS;
import static com.erp.admin.wms.calc.ShipProdCalcConstants.K_COEFF;
import static com.erp.admin.wms.calc.ShipProdCalcConstants.PROD_THRESHOLD_DAYS;
import static com.erp.admin.wms.calc.ShipProdCalcConstants.SAFETY_STOCK_DAYS;
import static com.erp.admin.wms.calc.ShipProdCalcConstants.SHIP_THRESHOLD_DAYS;

/**
 * 发货生产测算算法核心（纯函数，无 I/O）——移植自 model_core.py（2026-07 第二版）。
 * <p>
 * 与用户确认的口径：
 * <ul>
 *   <li>生产端消耗基准取 Z(t+1)，与发货端一致；</li>
 *   <li>ΔtE = τE − t；多批次在途/在制取最早批次日期，总量取全部；</li>
 *   <li>销量口径：全平台汇总、按 SKU 聚合。</li>
 * </ul>
 * 产能规划（逐月/双十一倒排）按用户要求不移植。
 *
 * @author erp
 */
public final class ShipProdCalcModel {

    private ShipProdCalcModel() {}

    // ================================================================ 销量指标

    /** [end-days+1, end] 窗口日均，缺失日按 0。 */
    static double windowAvg(Map<LocalDate, Integer> daily, LocalDate end, int days) {
        long total = 0;
        for (int i = 0; i < days; i++) {
            total += daily.getOrDefault(end.minusDays(i), 0);
        }
        return (double) total / days;
    }

    /** 首个有销量日到 end 的天数（衡量历史长度）。 */
    static int historyDays(Map<LocalDate, Integer> daily, LocalDate start, LocalDate end) {
        LocalDate min = null;
        for (Map.Entry<LocalDate, Integer> e : daily.entrySet()) {
            LocalDate d = e.getKey();
            if (e.getValue() != null && e.getValue() > 0
                    && !d.isBefore(start) && !d.isAfter(end)) {
                if (min == null || d.isBefore(min)) {
                    min = d;
                }
            }
        }
        if (min == null) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(min, end) + 1;
    }

    /** Xt = Avg7×0.5 + Avg15×0.3 + Avg30×0.2（窗口截止昨天）。返回 [xt, a7, a15, a30]。 */
    static double[] calcNormalDaily(Map<LocalDate, Integer> daily, LocalDate today) {
        LocalDate end = today.minusDays(1);
        double a7 = windowAvg(daily, end, 7);
        double a15 = windowAvg(daily, end, 15);
        double a30 = windowAvg(daily, end, 30);
        return new double[] { a7 * 0.5 + a15 * 0.3 + a30 * 0.2, a7, a15, a30 };
    }

    /**
     * Yt：过去一整年按 7 天分组取每组单日最大值，剔全局最大/最小后取均值。
     * 历史不足 21 天时退化为历史单日最大值（样本数记 0）。返回 [yt, samples]。
     */
    static double[] calcPeakDaily(Map<LocalDate, Integer> daily, LocalDate today) {
        LocalDate end = today.minusDays(1);
        LocalDate start = end.minusDays(364);
        List<Integer> peaks = new ArrayList<>();
        LocalDate gEnd = end;
        while (!gEnd.isBefore(start)) {
            LocalDate gStart = gEnd.minusDays(6);
            if (gStart.isBefore(start)) {
                gStart = start;
            }
            int peak = 0;
            long span = ChronoUnit.DAYS.between(gStart, gEnd);
            for (int i = 0; i <= span; i++) {
                peak = Math.max(peak, daily.getOrDefault(gStart.plusDays(i), 0));
            }
            peaks.add(peak);
            gEnd = gStart.minusDays(1);
        }

        if (historyDays(daily, start, end) < 21) {
            int mx = peaks.isEmpty() ? 0 : Collections.max(peaks);
            return new double[] { mx, 0 };
        }

        Collections.sort(peaks);
        List<Integer> trimmed = peaks.size() > 2 ? peaks.subList(1, peaks.size() - 1) : peaks;
        if (trimmed.isEmpty()) {
            return new double[] { 0.0, 0 };
        }
        long sum = 0;
        for (int p : trimmed) {
            sum += p;
        }
        return new double[] { (double) sum / trimmed.size(), trimmed.size() };
    }

    /**
     * Xt 数据窗口的等效季节系数 k_base（月初贴近上月系数，月中平滑过渡，不跳月）。
     * 第 i 天权重 w_i = 0.5/7(i&lt;7) + 0.3/15(i&lt;15) + 0.2/30(i&lt;30)，Σw=1。
     */
    static double windowBaseK(LocalDate today) {
        LocalDate end = today.minusDays(1);
        double kBase = 0.0;
        for (int i = 0; i < 30; i++) {
            LocalDate d = end.minusDays(i);
            double w = (i < 7 ? 0.5 / 7 : 0.0) + (i < 15 ? 0.3 / 15 : 0.0) + 0.2 / 30;
            kBase += w * K_COEFF.get(d.getMonthValue());
        }
        return kBase;
    }

    /** Z(t+a)：X(t+a) = Xt × k(t+a)/k_base，Z(t+a) = (X(t+a) + Yt) / 2。 */
    static double projectFutureZ(double xt, double yt, LocalDate today, int ahead) {
        double curK = windowBaseK(today);
        int futureMonth = ((today.getMonthValue() - 1 + ahead) % 12) + 1;
        double xFuture = xt * K_COEFF.get(futureMonth) / curK;
        return (xFuture + yt) / 2;
    }

    public static SalesMetrics calcSalesMetrics(Map<LocalDate, Integer> daily, LocalDate today) {
        double[] normal = calcNormalDaily(daily, today);
        double xt = normal[0];
        double[] peak = calcPeakDaily(daily, today);
        double yt = peak[0];
        double zt = (xt + yt) / 2;
        LocalDate end = today.minusDays(1);
        return SalesMetrics.builder()
                .xt(xt).yt(yt).zt(zt)
                .zt1(projectFutureZ(xt, yt, today, 1))
                .zt2(projectFutureZ(xt, yt, today, 2))
                .zt3(projectFutureZ(xt, yt, today, 3))
                .avg7(normal[1]).avg15(normal[2]).avg30(normal[3])
                .peakSamples((int) peak[1])
                .historyDays(historyDays(daily, end.minusDays(364), end))
                .kBase(windowBaseK(today))
                .build();
    }

    // ================================================================ 通用

    static double supportDays(double stock, double dailyRate) {
        if (dailyRate <= 0) {
            return stock >= 0 ? INF_DAYS : 0.0;
        }
        return stock / dailyRate;
    }

    /** 最早批次距今天数（已到期批次按 0 天）；无批次返回 null。 */
    static Double earliestEtaDays(List<CalcBatch> batches, LocalDate today) {
        LocalDate min = null;
        for (CalcBatch b : batches) {
            if (b.getEta() != null && b.getQty() > 0) {
                if (min == null || b.getEta().isBefore(min)) {
                    min = b.getEta();
                }
            }
        }
        if (min == null) {
            return null;
        }
        return Math.max(0.0, (double) ChronoUnit.DAYS.between(today, min));
    }

    private static int totalQty(List<CalcBatch> batches) {
        int total = 0;
        for (CalcBatch b : batches) {
            total += b.getQty();
        }
        return total;
    }

    // ================================================================ 发货测算

    public static ShippingResult calcShipping(SkuCalcInput inp, SalesMetrics m, LocalDate today) {
        double z1 = m.getZt1();
        int ab = inp.getOverseas() + inp.getFbo();
        int cTotal = totalQty(inp.getTransit());
        double s = supportDays(ab, z1);
        Double dtC = earliestEtaDays(inp.getTransit(), today);

        double sTotal;
        String path;
        if (cTotal <= 0 || dtC == null) {
            sTotal = s;
            path = "无在途：S_total = (A+B)/Z(t+1)";
        } else if (s < dtC) {
            sTotal = dtC + supportDays(cTotal, z1);
            path = "接力：现货撑不到最早到货，S_total = ΔtC + C/Z(t+1)";
        } else {
            sTotal = supportDays(ab + cTotal, z1);
            path = "全渠道：现货可撑到到货，S_total = (A+B+C)/Z(t+1)";
        }

        boolean need = sTotal < SHIP_THRESHOLD_DAYS;
        int plan = need ? (int) Math.round(m.getZt2() * SAFETY_STOCK_DAYS) : 0;
        return ShippingResult.builder()
                .s(s).dtC(dtC).sTotal(sTotal).needShip(need).planQty(plan).path(path)
                .build();
    }

    // ================================================================ 生产测算

    public static ProductionResult calcProduction(SkuCalcInput inp, SalesMetrics m, LocalDate today) {
        double z1 = m.getZt1();
        int baseStock = inp.getOverseas() + inp.getFbo() + totalQty(inp.getTransit()) + inp.getFactoryDone();
        int eTotal = totalQty(inp.getProducing());
        double sProd = supportDays(baseStock, z1);
        Double dtE = earliestEtaDays(inp.getProducing(), today);

        double sProdTotal;
        String path;
        if (eTotal <= 0 || dtE == null) {
            sProdTotal = sProd;
            path = "无在制：S_prod(total) = (A+B+C+D)/Z(t+1)";
        } else if (sProd < dtE + SHIP_THRESHOLD_DAYS) {
            sProdTotal = dtE + SHIP_THRESHOLD_DAYS + supportDays(eTotal, z1);
            path = "接力：现有链路撑不到完工+发运，S_prod(total) = ΔtE + 35 + E/Z(t+1)";
        } else {
            sProdTotal = supportDays(baseStock + eTotal, z1);
            path = "全渠道：S_prod(total) = (A+B+C+D+E)/Z(t+1)";
        }

        boolean need = sProdTotal < PROD_THRESHOLD_DAYS;
        int plan = need ? (int) Math.round(m.getZt3() * SAFETY_STOCK_DAYS) : 0;
        return ProductionResult.builder()
                .sProd(sProd).dtE(dtE).sProdTotal(sProdTotal).needProduce(need).planQty(plan).path(path)
                .build();
    }

    // ================================================================ 汇总入口

    public static SkuCalcResult evaluateSku(SkuCalcInput inp, Map<LocalDate, Integer> daily, LocalDate today) {
        SalesMetrics m = calcSalesMetrics(daily, today);
        List<String> warnings = new ArrayList<>();
        if (m.getHistoryDays() < 30) {
            warnings.add("历史仅 " + m.getHistoryDays() + " 天，销量指标可信度低（新品）");
        }
        if (m.getPeakSamples() == 0 && m.getYt() > 0) {
            warnings.add("历史不足 21 天，Yt 退化为历史单日最大值");
        }
        if (m.getZt1() <= 0) {
            warnings.add("估算日销为 0，支撑天数记为 ∞");
        }
        return SkuCalcResult.builder()
                .input(inp)
                .sales(m)
                .shipping(calcShipping(inp, m, today))
                .production(calcProduction(inp, m, today))
                .warnings(warnings)
                .build();
    }
}
