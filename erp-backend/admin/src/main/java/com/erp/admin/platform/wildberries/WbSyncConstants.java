package com.erp.admin.platform.wildberries;

/**
 * Wildberries 同步相关常量
 * 
 * @author system
 */
public final class WbSyncConstants {

    private WbSyncConstants() {
        throw new UnsupportedOperationException("Constant class");
    }

    /** 最大分页迭代次数（防止死循环） */
    public static final int MAX_PAGE_ITERATIONS = 10000;

    /** 最大安全分页次数（更保守的限制） */
    public static final int MAX_PAGES_SAFETY_LIMIT = 1000;

    /** 默认订单拉取页大小 */
    public static final int DEFAULT_ORDER_PAGE_SIZE = 1000;

    /** 默认批次拉取页大小 */
    public static final int DEFAULT_SUPPLY_PAGE_SIZE = 100;

    /** 默认仓库拉取页大小 */
    public static final int DEFAULT_OFFICE_PAGE_SIZE = 100;

    /** 默认状态批量查询大小 */
    public static final int DEFAULT_STATUS_BATCH_SIZE = 100;

    /** 默认订单窗口步长（天） */
    public static final int DEFAULT_WINDOW_STEP_DAYS = 30;

    /** 默认详情回溯天数 */
    public static final int DEFAULT_DETAILS_LOOKBACK_DAYS = 15;

    /** 默认首次同步回溯天数 */
    public static final int DEFAULT_FIRST_SYNC_LOOKBACK_DAYS = 365;

    /** 默认异常时回溯天数 */
    public static final int DEFAULT_ERROR_FALLBACK_DAYS = 30;
}

