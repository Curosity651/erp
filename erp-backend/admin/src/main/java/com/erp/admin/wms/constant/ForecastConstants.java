package com.erp.admin.wms.constant;

/**
 * 库存预测相关常量
 *
 * @author erp
 */
public final class ForecastConstants {

    /** 销量统计周期（天），用于计算日均销量 */
    public static final int SALES_STAT_DAYS = 30;

    /** 默认预测周期（天），前端未指定时使用 */
    public static final int DEFAULT_FORECAST_DAYS = 30;

    /** 近期销量天数（用于加权计算） */
    public static final int RECENT_DAYS = 7;

    /** 近期销量权重 */
    public static final int RECENT_WEIGHT = 2;

    /** 远期销量权重 */
    public static final int OLDER_WEIGHT = 1;

    private ForecastConstants() {}
}
