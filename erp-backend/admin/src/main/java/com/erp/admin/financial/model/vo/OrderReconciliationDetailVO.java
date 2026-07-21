package com.erp.admin.financial.model.vo;

import com.erp.admin.order.model.vo.OrderItemVO;
import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单财务对账详情 VO
 *
 * @author system
 */
@Data
@Schema(title = "订单财务对账详情")
public class OrderReconciliationDetailVO {

    // ==================== 订单信息 ====================

    @Schema(title = "订单ID")
    private Long orderId;

    @Schema(title = "平台订单号")
    private String platformOrderId;

    @Schema(title = "订单RID")
    private String rid;

    @Schema(title = "店铺ID")
    private Long shopId;

    @Schema(title = "店铺名称")
    private String shopName;

    @Schema(title = "履约类型", allowableValues = {"FBS", "FBO"})
    private String fulfillmentType;

    // ==================== SKU信息 ====================

    @Schema(title = "SKU简要信息")
    private SkuBriefVO skuBrief;

    @Schema(title = "订单商品明细")
    private List<OrderItemVO> items;

    // ==================== 状态信息（文本由前端处理） ====================

    @Schema(title = "ERP状态")
    private String erpStatus;

    @Schema(title = "商家处理状态")
    private String platformSubstatus;

    @Schema(title = "平台履约状态")
    private String platformStatus;

    // ==================== 金额与时间 ====================

    @Schema(title = "订单总金额（卢布）")
    private BigDecimal totalAmountRub;

    @Schema(title = "订单创建时间（莫斯科）")
    private LocalDateTime orderTimeMoscow;

    // ==================== 对账信息（文本由前端处理） ====================

    @Schema(title = "对账状态：MATCHED-已对账, PENDING-待履约, IN_TRANSIT-运输中, CANCELED-已取消, ANOMALY-异常")
    private String reconciliationStatus;

    // ==================== 财务汇总 ====================

    @Schema(title = "财务汇总信息")
    private FinancialSummary financialSummary;

    // ==================== 关联财务记录 ====================

    @Schema(title = "关联的财务记录列表")
    private List<FinancialRecordItemVO> financialRecords;

    /**
     * 财务汇总信息
     */
    @Data
    @Schema(title = "财务汇总信息")
    public static class FinancialSummary {

        @Schema(title = "财务记录总数")
        private Integer recordCount;

        @Schema(title = "销售记录数")
        private Integer saleRecordCount;

        @Schema(title = "退货记录数")
        private Integer returnRecordCount;

        @Schema(title = "销售金额（正向销售 ppvz_for_pay 合计）")
        private BigDecimal saleAmount;

        @Schema(title = "退货金额（退货类 ppvz_for_pay 合计，正数）")
        private BigDecimal returnAmount;

        @Schema(title = "罚款金额合计")
        private BigDecimal penaltyAmount;

        @Schema(title = "扣款金额合计")
        private BigDecimal deductionAmount;

        @Schema(title = "实际收入（净额）= 销售 - 退货 - 罚款 - 扣款")
        private BigDecimal actualIncome;

        @Schema(title = "币种名称")
        private String currencyName;
    }

    /**
     * 财务记录条目
     */
    @Data
    @Schema(title = "财务记录条目")
    public static class FinancialRecordItemVO {

        @Schema(title = "记录ID")
        private Long id;

        @Schema(title = "报表ID")
        private Long realizationreportId;

        @Schema(title = "报表周期类型")
        private String periodType;

        @Schema(title = "报表开始日期")
        private LocalDate dateFrom;

        @Schema(title = "报表结束日期")
        private LocalDate dateTo;

        @Schema(title = "文档类型名称")
        private String docTypeName;

        @Schema(title = "供应商操作名称")
        private String supplierOperName;

        @Schema(title = "奖惩类型名称")
        private String bonusTypeName;

        @Schema(title = "数量")
        private Integer quantity;

        @Schema(title = "应付金额（ppvz_for_pay）")
        private BigDecimal ppvzForPay;

        @Schema(title = "罚款金额")
        private BigDecimal penalty;

        @Schema(title = "扣款金额")
        private BigDecimal deduction;

        @Schema(title = "币种名称")
        private String currencyName;

        @Schema(title = "销售日期")
        private LocalDateTime saleDt;

        @Schema(title = "报表日期")
        private LocalDateTime rrDt;
    }
}
