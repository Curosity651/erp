package com.erp.admin.platform.ozon.model.request.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Ozon 包裹模型
 * 
 * @author system
 */
@Data
@Builder
public class OzonPackage {
    
    /**
     * 包裹中的产品列表
     */
    @JsonProperty("products")
    private List<OzonPackageProduct> products;
}
