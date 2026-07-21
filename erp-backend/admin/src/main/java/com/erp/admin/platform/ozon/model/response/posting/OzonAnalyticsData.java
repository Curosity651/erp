package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ozon 分析数据模型
 * 
 * @author system
 */
@Data
public class OzonAnalyticsData {
    
    /**
     * 地区
     */
    @JsonProperty("region")
    private String region;
    
    /**
     * 城市
     */
    @JsonProperty("city")
    private String city;
    
    /**
     * 配送类型
     */
    @JsonProperty("delivery_type")
    private String deliveryType;
    
    /**
     * 是否为首次购买
     */
    @JsonProperty("is_first_order")
    private Boolean isFirstOrder;
    
    /**
     * 配送日期开始
     */
    @JsonProperty("delivery_date_begin")
    private String deliveryDateBegin;
    
    /**
     * 配送日期结束
     */
    @JsonProperty("delivery_date_end")
    private String deliveryDateEnd;
    
    /**
     * 仓库ID (FBO订单使用)
     */
    @JsonProperty("warehouse_id")
    private Long warehouseId;
    
    /**
     * 仓库名称 (FBO订单使用)
     */
    @JsonProperty("warehouse_name")
    private String warehouseName;
    
    /**
     * 是否为高级会员订单
     */
    @JsonProperty("is_premium")
    private Boolean isPremium;
    
    /**
     * 支付类型组名称
     */
    @JsonProperty("payment_type_group_name")
    private String paymentTypeGroupName;
    
    /**
     * 是否为法人订单
     */
    @JsonProperty("is_legal")
    private Boolean isLegal;
}
