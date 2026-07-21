package com.erp.admin.financial.model.vo;

import com.erp.admin.order.model.vo.OrderItemVO;
import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单财务对账列表 VO
 *
 * @author system
 */
@Data
@Schema(title = "订单财务对账列表")
public class OrderReconciliationVO {

    // ==================== 订单基础信息 ====================

    @Schema(title = "订单ID")
    private Long orderId;

    @Schema(title = "平台名称", example = "wildberries")
    private String platform;

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

    @Deprecated // TODO 迭代 7 删除
    @Schema(title = "商品编码 (article)")
    private String article;

    @Deprecated // TODO 迭代 7 删除
    @Schema(title = "商品数量")
    private Integer quantity;

    // ==================== SKU信息 ====================

    @Schema(title = "SKU简要信息")
    private SkuBriefVO skuBrief;

    @Schema(title = "订单商品明细")
    private List<OrderItemVO> items;

    // ==================== 三状态信息（文本由前端处理） ====================

    @Schema(title = "ERP状态")
    private String erpStatus;

    @Schema(title = "商家处理状态（platformSubstatus/supplierStatus）")
    private String platformSubstatus;

    @Schema(title = "平台履约状态（platformStatus/wbStatus）")
    private String platformStatus;

    // ==================== 对账状态信息（文本由前端处理） ====================

    @Schema(title = "对账状态：MATCHED-已对账, PENDING-待履约, IN_TRANSIT-运输中, CANCELED-已取消, ANOMALY-异常")
    private String reconciliationStatus;

    // ==================== 财务汇总信息 ====================

    @Schema(title = "财务记录数量")
    private Integer financialRecordCount;

    @Schema(title = "正向销售金额（supplier_oper_name 为销售的 ppvz_for_pay 合计）")
    private BigDecimal saleAmount;

    @Schema(title = "实收金额（所有财务记录 ppvz_for_pay 合计）")
    private BigDecimal actualIncome;

    @Schema(title = "财务记录币种", example = "руб")
    private String financialCurrencyName;

    // ==================== 金额与时间 ====================

    @Schema(title = "订单时间（莫斯科时区）")
    private LocalDateTime orderTimeMoscow;

    @Schema(title = "订单总金额（原始币种）")
    private BigDecimal totalAmount;

    @Schema(title = "订单总金额（卢布）")
    private BigDecimal totalAmountRub;

    @Schema(title = "货币代码", example = "RUB")
    private String currencyCode;

    @Schema(title = "转换后金额（CNY）")
    private BigDecimal convertedAmount;

    @Schema(title = "转换后货币代码", example = "CNY")
    private String convertedCurrencyCode;
}
