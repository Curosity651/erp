package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ozon 客户信息模型
 * 
 * @author system
 */
@Data
public class OzonCustomer {
    
    /**
     * 客户 ID
     */
    @JsonProperty("customer_id")
    private Long customerId;
    
    /**
     * 客户姓名
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * 客户电话
     */
    @JsonProperty("phone")
    private String phone;
    
    /**
     * 客户邮箱
     */
    @JsonProperty("email")
    private String email;
}
