package com.erp.admin.platform.ozon.model.request.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Ozon 订单详情请求模型
 * 
 * @author system
 */
@Data
@Builder
public class OzonPostingGetRequest {
    
    /**
     * 发货单号
     */
    @JsonProperty("posting_number")
    private String postingNumber;
    
    /**
     * 附加数据选项
     */
    @JsonProperty("with")
    private OzonPostingWith with;
}
