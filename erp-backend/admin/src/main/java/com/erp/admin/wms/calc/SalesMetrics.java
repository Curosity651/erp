package com.erp.admin.wms.calc;

import lombok.Builder;
import lombok.Data;

/**
 * 销量指标输出（移植自 model_core.SalesMetrics）。
 *
 * @author erp
 */
@Data
@Builder
public class SalesMetrics {

    /** Xt 常态日均 = Avg7×0.5 + Avg15×0.3 + Avg30×0.2 */
    private double xt;

    /** Yt 巅峰日均（365 天分组峰值剔极值均值） */
    private double yt;

    /** Zt 估算日均 = (Xt + Yt) / 2 */
    private double zt;

    /** Z(t+1) 次月估算日均 */
    private double zt1;

    /** Z(t+2) */
    private double zt2;

    /** Z(t+3) */
    private double zt3;

    private double avg7;
    private double avg15;
    private double avg30;

    /** Yt 剔除极值后参与平均的样本数 */
    private int peakSamples;

    /** 可用历史天数（诊断新品） */
    private int historyDays;

    /** 销量窗口加权季节系数（推算分母 k_base） */
    private double kBase;
}
