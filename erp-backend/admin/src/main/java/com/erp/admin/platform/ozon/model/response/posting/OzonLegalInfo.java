package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ozon 法律信息模型
 * 
 * @author system
 */
@Data
public class OzonLegalInfo {
    
    /**
     * 公司名称
     */
    @JsonProperty("company_name")
    private String companyName;
    
    /**
     * 税号 (INN)
     */
    @JsonProperty("inn")
    private String inn;
    
    /**
     * 税务登记原因代码 (KPP)
     */
    @JsonProperty("kpp")
    private String kpp;
}
