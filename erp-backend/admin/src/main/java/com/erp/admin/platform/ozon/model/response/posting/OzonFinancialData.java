package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * Ozon 财务数据模型
 * 
 * @author system
 */
@Data
public class OzonFinancialData {
    
    /**
     * 发货仓库集群
     */
    @JsonProperty("cluster_from")
    private String clusterFrom;
    
    /**
     * 收货仓库集群
     */
    @JsonProperty("cluster_to")
    private String clusterTo;
    
    /**
     * 产品财务数据列表
     */
    @JsonProperty("products")
    private List<OzonFinancialProduct> products;
}
