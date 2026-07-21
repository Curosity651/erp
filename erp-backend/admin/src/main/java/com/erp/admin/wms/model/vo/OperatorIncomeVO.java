package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 服务商财务-收入视图（链路二：物流产品使用计费）。
 *
 * @author erp
 */
public class OperatorIncomeVO {

    private OperatorIncomeVO() {
    }

    /** 收入汇总（统计卡 + 按产品×月汇总行） */
    @Data
    @Schema(title = "收入汇总")
    public static class Summary {
        @Schema(title = "区间收入合计")
        private BigDecimal totalAmount;

        @Schema(title = "区间使用总次数")
        private Long totalCount;

        @Schema(title = "按产品×月汇总行")
        private List<SummaryRow> rows;
    }

    /** 按 产品×月 汇总行 */
    @Data
    @Schema(title = "收入汇总行")
    public static class SummaryRow {
        private Long productId;

        private String productName;

        @Schema(title = "特性词条")
        private List<String> tags;

        @Schema(title = "单价")
        private BigDecimal unitPrice;

        @Schema(title = "账期 YYYY-MM")
        private String billMonth;

        @Schema(title = "使用次数")
        private Long usageCount;

        @Schema(title = "小计")
        private BigDecimal subtotal;
    }

    /** 明细流水行 */
    @Data
    @Schema(title = "收入明细流水")
    public static class Record {
        private Long id;

        private String billMonth;

        private Long productId;

        private String productName;

        @Schema(title = "货主名称")
        private String ownerName;

        @Schema(title = "出库单号")
        private String outboundNo;

        @Schema(title = "跟踪号")
        private String trackingNo;

        private BigDecimal amount;

        private String createTime;
    }

}
