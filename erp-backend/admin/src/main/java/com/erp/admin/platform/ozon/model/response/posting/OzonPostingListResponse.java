package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * Ozon 订单列表响应模型
 * 
 * @author system
 */
@Data
public class OzonPostingListResponse {
    
    /**
     * 结果数据
     */
    @JsonProperty("result")
    private OzonPostingListResult result;
    
    @Data
    public static class OzonPostingListResult {
        /**
         * 订单列表
         */
        @JsonProperty("postings")
        private List<OzonPosting> postings;
        
        /**
         * 是否还有下一页
         */
        @JsonProperty("has_next")
        private Boolean hasNext;
    }
}
