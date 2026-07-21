package com.erp.admin.platform.wildberries.model.response.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Wildberries 订单列表响应
 * <p>
 * 对应 API: GET /api/v3/orders
 * 
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WbOrderListResponse {
    
    /** 订单列表 */
    @JsonProperty("orders")
    @Builder.Default
    private List<WbOrder> orders = new ArrayList<>();
    
    /** 下一页游标，null 表示没有更多数据 */
    @JsonProperty("next")
    private Long next;
}
