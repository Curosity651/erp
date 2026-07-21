package com.erp.admin.platform.wildberries.model.response.supply;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wildberries 创建 Supply 响应
 * <p>
 * 对应 API: POST /api/v3/supplies
 * 
 * @author system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WbCreateSupplyResponse {
    
    /** 新创建的 Supply ID */
    @JsonProperty("id")
    private String id;
}
