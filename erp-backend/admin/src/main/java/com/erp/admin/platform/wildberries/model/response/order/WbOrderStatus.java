package com.erp.admin.platform.wildberries.model.response.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wildberries 订单状态信息
 * <p>
 * 对应 API: POST /api/v3/orders/status 的响应项
 * <p>
 * 注意：订单状态需要单独调用此接口获取，GET /api/v3/orders 不返回状态
 * 
 * @author system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WbOrderStatus {
    
    /** 订单ID */
    @JsonProperty("id")
    private Long id;
    
    /** 商家侧状态：new, confirm, complete, cancel, receive, reject */
    @JsonProperty("supplierStatus")
    private String supplierStatus;
    
    /** WB 平台状态：waiting, sorted, sold, canceled, canceled_by_client, declined_by_client, defect, ready_for_pickup, postponed_delivery */
    @JsonProperty("wbStatus")
    private String wbStatus;
}
