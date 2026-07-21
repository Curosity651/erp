package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * WMS 服务商运营数据分析看板（900400）总返回。
 * A 经营总览 / B 收支趋势(按月) / C 产品分析 / D 货主分析 / E 服务规模。
 *
 * @author erp
 */
@Data
@Schema(title = "服务商运营看板数据")
public class OperatorDashboardVO {

    private Overview overview;

    private Trend trend;

    @Schema(title = "产品分析(按收入降序，柱图取次数/环形取金额)")
    private List<ProductStat> products;

    @Schema(title = "货主收入贡献 TOP10")
    private List<OwnerStat> ownerIncomeTop;

    @Schema(title = "货主出库吞吐 TOP10")
    private List<OwnerOrderStat> ownerOutboundTop;

    private Scale scale;

    /** A 经营总览 */
    @Data
    public static class Overview {
        @Schema(title = "本期收入")
        private BigDecimal income;

        @Schema(title = "本期产品使用次数")
        private long incomeCount;

        @Schema(title = "本期支出(应付平台)")
        private BigDecimal expense;

        @Schema(title = "净收益(收入-支出)")
        private BigDecimal netProfit;

        private long ownerTotal;

        private long ownerEnabled;

        private long productTotal;

        private long productEnabled;
    }

    /** B 收支趋势（按月，三数组与 months 等长对齐） */
    @Data
    public static class Trend {
        private List<String> months;

        private List<BigDecimal> income;

        private List<BigDecimal> expense;

        private List<BigDecimal> net;
    }

    /** C 产品统计行 */
    @Data
    public static class ProductStat {
        private Long productId;

        private String productName;

        private long usageCount;

        private BigDecimal amount;
    }

    /** D 货主收入贡献行 */
    @Data
    public static class OwnerStat {
        private Long erpTenantId;

        private String ownerName;

        private BigDecimal amount;

        private long usageCount;
    }

    /** D 货主出库吞吐行 */
    @Data
    public static class OwnerOrderStat {
        private Long erpTenantId;

        private String ownerName;

        private long orders;
    }

    /** E 服务规模 */
    @Data
    public static class Scale {
        @Schema(title = "名下货主在库总件数")
        private long onHandQty;

        @Schema(title = "当前有效货架数")
        private long rackCount;

        @Schema(title = "货架月租成本合计")
        private BigDecimal rackMonthlyFee;
    }

}
