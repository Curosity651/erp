package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * Ozon 产品模型
 * 
 * @author system
 */
@Data
public class OzonProduct {
    
    /**
     * 卖家商品 ID (Offer ID)
     */
    @JsonProperty("offer_id")
    private String offerId;
    
    /**
     * Ozon SKU
     */
    @JsonProperty("sku")
    private Long sku;
    
    /**
     * 产品名称
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * 数量
     */
    @JsonProperty("quantity")
    private Integer quantity;
    
    /**
     * 价格
     */
    @JsonProperty("price")
    private String price;
    
    /**
     * 货币代码
     */
    @JsonProperty("currency_code")
    private String currencyCode;
    
    /**
     * 是否为市场回购
     */
    @JsonProperty("is_marketplace_buyout")
    private Boolean isMarketplaceBuyout;
    
    /**
     * 是否为白俄罗斯可追溯商品
     */
    @JsonProperty("is_blr_traceable")
    private Boolean isBlrTraceable;
    
    /**
     * 是否有 IMEI
     */
    @JsonProperty("has_imei")
    private Boolean hasImei;
    
    /**
     * IMEI 列表
     */
    @JsonProperty("imei")
    private List<String> imei;
    
    /**
     * 产品尺寸
     */
    @JsonProperty("dimensions")
    private OzonDimensions dimensions;
    
    @Data
    public static class OzonDimensions {
        /**
         * 高度 (mm)
         */
        @JsonProperty("height")
        private String height;
        
        /**
         * 长度 (mm)
         */
        @JsonProperty("length")
        private String length;
        
        /**
         * 宽度 (mm)
         */
        @JsonProperty("width")
        private String width;
        
        /**
         * 重量 (g)
         */
        @JsonProperty("weight")
        private String weight;
    }
}
