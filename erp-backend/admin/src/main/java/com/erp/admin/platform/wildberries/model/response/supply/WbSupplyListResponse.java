package com.erp.admin.platform.wildberries.model.response.supply;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Wildberries Supply 列表响应
 * <p>
 * 对应 API: GET /api/v3/supplies
 * 
 * @author system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WbSupplyListResponse {
    
    /** Supply 列表 */
    @JsonProperty("supplies")
    @Builder.Default
    private List<WbSupplyDetail> supplies = new ArrayList<>();
    
    /** 下一页游标，null 表示没有更多数据 */
    @JsonProperty("next")
    private Long next;
}
