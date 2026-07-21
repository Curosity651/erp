package com.erp.admin.platform.wildberries.model.request.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Wildberries 订单状态查询请求
 * <p>
 * 对应 API: POST /api/v3/orders/status
 * 
 * @author system
 */
@Data
@Builder
public class WbOrderStatusRequest {
    
    /** 订单ID列表（最多 1000 个） */
    @JsonProperty("orders")
    private List<Long> orders;
}
