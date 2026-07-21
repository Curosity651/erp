package com.erp.admin.platform.ozon.model.response.label;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Base64;

/**
 * Ozon 面单响应模型
 * <p>
 * API: POST /v2/posting/fbs/package-label
 * <p>
 * 响应包含 Base64 编码的 PDF 文件内容
 * 
 * @author system
 */
@Data
@Builder
public class OzonPackageLabelResponse {
    
    /**
     * 内容类型
     * 通常为 "application/pdf"
     */
    @JsonProperty("content_type")
    private String contentType;
    
    /**
     * 文件名
     * 例如: "ticket-170660-2023-07-13T13:17:06Z.pdf"
     */
    @JsonProperty("file_name")
    private String fileName;
    
    /**
     * pdf 文件内容的字节数组
     */
    @JsonProperty("file_content")
    private byte[] fileContent;

}
