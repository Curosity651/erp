package com.erp.admin.platform.ozon.model.request.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Ozon FBO 订单详情请求模型
 * <p>
 * API: POST /v2/posting/fbo/get
 *
 * @author system
 */
@Data
@Builder
public class OzonFboPostingGetRequest {

    /**
     * 发货单号
     */
    @JsonProperty("posting_number")
    private String postingNumber;

    /**
     * 附加数据
     */
    @JsonProperty("with")
    private OzonPostingWith with;
}
