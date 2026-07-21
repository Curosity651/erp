package com.erp.admin.wms.calc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 发货生产测算模型常量（移植自 model_core.py 顶部常量）。
 * <p>
 * 口径可变的参数（月度系数 / 阈值 / 备货天数 / 窗口权重）集中于此，后续可外置为配置。
 * 与 model_core.py（2026-07 第二版）严格一致。
 *
 * @author erp
 */
public final class ShipProdCalcConstants {

    private ShipProdCalcConstants() {}

    /** 各月份销售系数 kt —— 2026-07-02 用户提供新版（来源：月度销量占比表）。 */
    public static final Map<Integer, Double> K_COEFF;
    static {
        Map<Integer, Double> k = new HashMap<>();
        k.put(1, 2.15);  k.put(2, 1.05);  k.put(3, 1.16);
        k.put(4, 1.11);  k.put(5, 1.00);  k.put(6, 1.05);
        k.put(7, 1.32);  k.put(8, 1.74);  k.put(9, 1.42);
        k.put(10, 1.63); k.put(11, 1.84); k.put(12, 2.53);
        K_COEFF = Collections.unmodifiableMap(k);
    }

    /** 发货决策阈值（天）：全链路支撑 < 35 天则需发货。 */
    public static final int SHIP_THRESHOLD_DAYS = 35;

    /** 生产决策阈值（天）：全渠道支撑 < 80 天则需订货。 */
    public static final int PROD_THRESHOLD_DAYS = 80;

    /** 备货安全天数（F 发货量 / Q 订货量共用）。 */
    public static final int SAFETY_STOCK_DAYS = 45;

    /** 销量统计周期（天），用于日均销量。 */
    public static final int SALES_STAT_DAYS = 30;

    /** 无限支撑天数（零销量时）。 */
    public static final double INF_DAYS = Double.POSITIVE_INFINITY;
}
