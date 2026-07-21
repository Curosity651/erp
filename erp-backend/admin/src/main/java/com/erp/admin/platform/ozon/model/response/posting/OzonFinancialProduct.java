package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Ozon 产品财务数据模型
 * <p>
 * 适用于 FBS 和 FBO 订单
 * - FBS 订单: 包含 customer_price 字段
 * - FBO 订单: 包含 old_price, total_discount_value, total_discount_percent, actions, quantity 字段
 * 
 * @author system
 */
@Data
public class OzonFinancialProduct {
    
    /**
     * 产品 ID (SKU)
     */
    @JsonProperty("product_id")
    private Long productId;
    
    /**
     * 佣金金额
     */
    @JsonProperty("commission_amount")
    private String commissionAmount;
    
    /**
     * 佣金百分比
     */
    @JsonProperty("commission_percent")
    private String commissionPercent;
    
    /**
     * 支付金额 (卖家实际收款)
     */
    @JsonProperty("payout")
    private String payout;
    
    /**
     * 价格
     * <p>
     * FBS: 商品价格
     * FBO: 售价(包含折扣后的价格)
     */
    @JsonProperty("price")
    private String price;
    
    /**
     * 货币代码
     */
    @JsonProperty("currency_code")
    private String currencyCode;
    
    // ==================== FBS 独有字段 ====================
    
    /**
     * 客户支付价格 (FBS 订单)
     * <p>
     * FBO 订单中此字段为 null
     */
    @JsonProperty("customer_price")
    private String customerPrice;

	/**
	 * 原价 (FBO 订单)
	 * <p>
	 * 折扣前的价格
	 */
	@JsonProperty("old_price")
	private String oldPrice;
    
    // ==================== FBO 独有字段 ====================

    /**
     * 总折扣金额 (FBO 订单)
     */
    @JsonProperty("total_discount_value")
    private String totalDiscountValue;
    
    /**
     * 总折扣百分比 (FBO 订单)
     */
    @JsonProperty("total_discount_percent")
    private String totalDiscountPercent;
    
    /**
     * 促销活动列表 (FBO 订单)
     * <p>
     * 例如: ["Системная виртуальная скидка селлера"]
     */
    @JsonProperty("actions")
    private List<String> actions;
    
    /**
     * 数量 (FBO 订单)
     */
    @JsonProperty("quantity")
    private Integer quantity;
}
