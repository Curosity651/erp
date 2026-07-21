package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ozon FBO 订单详情响应模型
 * <p>
 * API: POST /v2/posting/fbo/get
 *
 * @author system
 */
@Data
public class OzonFboPostingGetResponse {

    /**
     * 结果数据
     */
    @JsonProperty("result")
    private OzonFboPosting result;
}
