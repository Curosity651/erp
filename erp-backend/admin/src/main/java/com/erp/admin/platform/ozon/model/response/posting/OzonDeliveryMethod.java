package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ozon 配送方式模型
 * 
 * @author system
 */
@Data
public class OzonDeliveryMethod {
    
    /**
     * 配送方式 ID
     */
    @JsonProperty("id")
    private Long id;
    
    /**
     * 配送方式名称
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * 仓库 ID
     */
    @JsonProperty("warehouse_id")
    private Long warehouseId;
    
    /**
     * 仓库名称
     */
    @JsonProperty("warehouse")
    private String warehouse;
    
    /**
     * 物流服务商 ID
     */
    @JsonProperty("tpl_provider_id")
    private Long tplProviderId;
    
    /**
     * 物流服务商名称
     */
    @JsonProperty("tpl_provider")
    private String tplProvider;
}
