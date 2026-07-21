package com.erp.admin.platform.wildberries.model.response.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Wildberries 订单状态批量查询响应
 * <p>
 * 对应 API: POST /api/v3/orders/status
 * 
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WbOrderStatusResponse {
    
    /** 订单状态列表 */
    @JsonProperty("orders")
    @Builder.Default
    private List<WbOrderStatus> orders = new ArrayList<>();
}
