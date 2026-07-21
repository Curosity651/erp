package com.erp.admin.platform.ozon.model.request.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Ozon 订单列表请求模型
 * 
 * @author system
 */
@Data
@Builder
public class OzonPostingListRequest {
    
    /**
     * 过滤条件
     */
    @JsonProperty("filter")
    private OzonPostingFilter filter;
    
    /**
     * 每页数量限制
     * 范围：1-1000
     */
    @JsonProperty("limit")
    private Integer limit;
    
    /**
     * 偏移量
     * 从 0 开始
     */
    @JsonProperty("offset")
    private Integer offset;
    
    /**
     * 排序方向
     * ASC 或 DESC
     */
    @JsonProperty("dir")
    private String dir;
    
    /**
     * 附加数据选项
     */
    @JsonProperty("with")
    private OzonPostingWith with;
}
