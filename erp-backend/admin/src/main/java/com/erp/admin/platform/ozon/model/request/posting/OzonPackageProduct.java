package com.erp.admin.platform.ozon.model.request.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Ozon 包裹产品模型
 * 
 * @author system
 */
@Data
@Builder
public class OzonPackageProduct {
    
    /**
     * 产品 ID (SKU)
     */
    @JsonProperty("product_id")
    private Long productId;
    
    /**
     * 数量
     */
    @JsonProperty("quantity")
    private Integer quantity;
}
