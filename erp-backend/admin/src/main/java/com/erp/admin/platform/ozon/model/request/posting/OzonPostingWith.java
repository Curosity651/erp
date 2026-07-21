package com.erp.admin.platform.ozon.model.request.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Ozon 订单附加数据选项
 * 
 * @author system
 */
@Data
@Builder
public class OzonPostingWith {
    
    /**
     * 是否包含分析数据
     */
    @JsonProperty("analytics_data")
    private Boolean analyticsData;
    
    /**
     * 是否包含条码信息
     */
    @JsonProperty("barcodes")
    private Boolean barcodes;
    
    /**
     * 是否包含财务数据
     */
    @JsonProperty("financial_data")
    private Boolean financialData;
    
    /**
     * 是否包含音译信息
     */
    @JsonProperty("translit")
    private Boolean translit;
    
    /**
     * 是否包含法律信息
     */
    @JsonProperty("legal_info")
    private Boolean legalInfo;
    
    /**
     * 是否包含产品示例信息
     */
    @JsonProperty("product_exemplars")
    private Boolean productExemplars;
    
    /**
     * 是否包含关联订单信息
     */
    @JsonProperty("related_postings")
    private Boolean relatedPostings;
}
