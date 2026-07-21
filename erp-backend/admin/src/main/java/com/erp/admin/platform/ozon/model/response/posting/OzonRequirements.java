package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * Ozon 需求信息模型
 * 
 * @author system
 */
@Data
public class OzonRequirements {
    
    /**
     * 需要更改国家的产品列表
     */
    @JsonProperty("products_requiring_change_country")
    private List<Long> productsRequiringChangeCountry;
    
    /**
     * 需要 GTD 的产品列表
     */
    @JsonProperty("products_requiring_gtd")
    private List<Long> productsRequiringGtd;
    
    /**
     * 需要国家信息的产品列表
     */
    @JsonProperty("products_requiring_country")
    private List<Long> productsRequiringCountry;
    
    /**
     * 需要强制标记的产品列表
     */
    @JsonProperty("products_requiring_mandatory_mark")
    private List<Long> productsRequiringMandatoryMark;
    
    /**
     * 需要 IMEI 的产品列表
     */
    @JsonProperty("products_requiring_imei")
    private List<Long> productsRequiringImei;
}
