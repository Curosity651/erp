package com.erp.admin.platform.wildberries.model.response.supply;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wildberries Supply（发货批次）响应模型
 * <p>
 * 对应 API: GET /api/v3/supplies, GET /api/v3/supplies/{supplyId}
 * <p>
 * Supply 是 WB 特有的概念，订单需要先加入 Supply 才能发货
 * 
 * @author system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WbSupplyDetail {
    
    /** Supply ID */
    @JsonProperty("id")
    private String id;
    
    /** Supply 名称 */
    @JsonProperty("name")
    private String name;
    
    /** 是否已关闭 */
    @JsonProperty("done")
    private Boolean done;
    
    /** 创建时间（UTC 时区） */
    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime createdAt;
    
    /** 关闭时间（UTC 时区，可为 null） */
    @JsonProperty("closedAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime closedAt;
    
    /** 扫描时间（UTC 时区，可为 null） */
    @JsonProperty("scanDt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime scanDt;
    
    /** 货物类型：0=未指定, 1=小件, 2=大件, 3=超大件 */
    @JsonProperty("cargoType")
    private Integer cargoType;
    
    /** 目标办公点ID（可为 null） */
    @JsonProperty("destinationOfficeId")
    private Long destinationOfficeId;
}
