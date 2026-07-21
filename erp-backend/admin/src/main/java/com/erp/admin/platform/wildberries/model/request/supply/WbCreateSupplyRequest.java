package com.erp.admin.platform.wildberries.model.request.supply;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Wildberries 创建 Supply 请求
 * <p>
 * 对应 API: POST /api/v3/supplies
 * 
 * @author system
 */
@Data
@Builder
public class WbCreateSupplyRequest {
    
    /** Supply 名称（可选，长度 1-128） */
    @JsonProperty("name")
    private String name;
}
