package com.erp.admin.platform.dashboard.model.vo;

import lombok.Data;

/**
 * 数据分析聚合的中间行 VO（mapper 结果，Service 再组装成看板 VO）。
 *
 * @author erp
 */
public class DashboardRowVO {

    private DashboardRowVO() {
    }

    /** 在库总量行 */
    @Data
    public static class StockRow {
        private long onHandQty;
        private long skuCount;
        private long ownerCount;
    }

    /** 单据数量/件数行（今日入/出库） */
    @Data
    public static class MetricRow {
        private long qty;
        private long orders;
    }

    /** 按日计数行 */
    @Data
    public static class DailyCountRow {
        private String bizDate;
        private long cnt;
    }

}
