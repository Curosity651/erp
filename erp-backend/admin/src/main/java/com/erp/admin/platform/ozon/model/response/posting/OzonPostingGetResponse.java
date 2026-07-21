package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ozon 订单详情响应模型
 * 
 * @author system
 */
@Data
public class OzonPostingGetResponse {
    
    /**
     * 订单详情
     */
    @JsonProperty("result")
    private OzonPosting result;
}
