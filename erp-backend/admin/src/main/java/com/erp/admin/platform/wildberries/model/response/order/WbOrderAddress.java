package com.erp.admin.platform.wildberries.model.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wildberries 订单配送地址
 * <p>
 * 仅部分订单包含此信息（如需要精确配送地址的订单）
 * 
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WbOrderAddress {
    
    /** 完整配送地址 */
    @JsonProperty("fullAddress")
    private String fullAddress;
    
    /** 经度 */
    @JsonProperty("longitude")
    private Double longitude;
    
    /** 纬度 */
    @JsonProperty("latitude")
    private Double latitude;
}
