package com.erp.admin.platform.wildberries.model.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wildberries API 错误响应模型
 * <p>
 * 对应 WB API 的标准错误响应格式（401、429 等错误）
 * 
 * @author system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WbErrorResponse {
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("detail")
    private String detail;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("requestId")
    private String requestId;
    
    @JsonProperty("origin")
    private String origin;
    
    @JsonProperty("status")
    private Integer status;
    
    @JsonProperty("statusText")
    private String statusText;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime timestamp;
}
