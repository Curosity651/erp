package com.erp.admin.platform.ozon.model.request.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Ozon FBO 订单列表请求模型
 * <p>
 * API: POST /v2/posting/fbo/list
 * <p>
 * FBO (Fulfillment by Ozon) 订单由 Ozon 仓库发货
 *
 * @author system
 */
@Data
@Builder
public class OzonFboPostingListRequest {

    /**
     * 过滤条件
     */
    @JsonProperty("filter")
    private OzonFboPostingFilter filter;

    /**
     * 每页数量（1-1000）
     */
    @JsonProperty("limit")
    private Integer limit;

    /**
     * 偏移量
     */
    @JsonProperty("offset")
    private Integer offset;

    /**
     * 排序方向（ASC/DESC）
     */
    @JsonProperty("dir")
    private String dir;

    /**
     * 附加数据
     */
    @JsonProperty("with")
    private OzonPostingWith with;
}
