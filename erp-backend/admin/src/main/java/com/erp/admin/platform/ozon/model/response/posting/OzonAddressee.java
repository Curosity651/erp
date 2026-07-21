package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ozon 收件人信息模型
 * 
 * @author system
 */
@Data
public class OzonAddressee {
    
    /**
     * 收件人姓名
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * 收件人电话
     */
    @JsonProperty("phone")
    private String phone;
    
    /**
     * 收件地址
     */
    @JsonProperty("address")
    private OzonAddress address;
    
    @Data
    public static class OzonAddress {
        /**
         * 地址文本
         */
        @JsonProperty("address_tail")
        private String addressTail;
        
        /**
         * 城市
         */
        @JsonProperty("city")
        private String city;
        
        /**
         * 地区
         */
        @JsonProperty("district")
        private String district;
        
        /**
         * 纬度
         */
        @JsonProperty("latitude")
        private Double latitude;
        
        /**
         * 经度
         */
        @JsonProperty("longitude")
        private Double longitude;
        
        /**
         * 地区 ID
         */
        @JsonProperty("region_id")
        private Long regionId;
        
        /**
         * 邮政编码
         */
        @JsonProperty("zip_code")
        private String zipCode;
    }
}
