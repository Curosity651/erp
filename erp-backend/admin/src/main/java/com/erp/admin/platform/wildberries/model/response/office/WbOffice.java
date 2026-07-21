package com.erp.admin.platform.wildberries.model.response.office;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wildberries Office（办公点/仓库）响应模型
 * <p>
 * 对应 API: GET /api/v3/offices
 * <p>
 * 注意：此 API 直接返回数组，不是对象包装
 * 
 * @author system
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class WbOffice {
    
    /** Office ID */
    @JsonProperty("id")
    private Long id;
    
    /** 名称 */
    @JsonProperty("name")
    private String name;
    
    /** 城市 */
    @JsonProperty("city")
    private String city;
    
    /** 地址 */
    @JsonProperty("address")
    private String address;
    
    /** 经度 */
    @JsonProperty("longitude")
    private Double longitude;
    
    /** 纬度 */
    @JsonProperty("latitude")
    private Double latitude;
    
    /** 可接受货物类型：1=小件, 2=大件, 3=超大件 */
    @JsonProperty("cargoType")
    private Integer cargoType;
    
    /** 配送类型：1=FBS, 2=DBS, 3=DBW, 5=C&C, 6=EDBS */
    @JsonProperty("deliveryType")
    private Integer deliveryType;
    
    /** 联邦区（可为 null，俄罗斯境外或未指定） */
    @JsonProperty("federalDistrict")
    private String federalDistrict;
    
    /** 是否已被商家选择 */
    @JsonProperty("selected")
    private Boolean selected;
}
