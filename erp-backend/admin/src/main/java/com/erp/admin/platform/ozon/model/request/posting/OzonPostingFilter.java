package com.erp.admin.platform.ozon.model.request.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Ozon 订单过滤条件
 * 
 * @author system
 */
@Data
@Builder
public class OzonPostingFilter {
    
    /**
     * 开始时间
     * 时间范围不能超过一年
     */
    @JsonProperty("since")
    private OffsetDateTime since;
    
    /**
     * 结束时间
     * 时间范围不能超过一年
     */
    @JsonProperty("to")
    private OffsetDateTime to;
    
    /**
     * 订单状态
     * 可选值：
     * - awaiting_packaging: 等待打包
     * - awaiting_deliver: 等待发货
     * - delivering: 配送中
     * - delivered: 已送达
     * - cancelled: 已取消
     */
    @JsonProperty("status")
    private String status;
    
    /**
     * 配送方式 ID 列表
     */
    @JsonProperty("delivery_method_id")
    private List<Long> deliveryMethodId;
    
    /**
     * 仓库 ID 列表
     */
    @JsonProperty("warehouse_id")
    private List<Long> warehouseId;
    
    /**
     * 物流服务商 ID 列表
     */
    @JsonProperty("provider_id")
    private List<Long> providerId;
    
    /**
     * 订单 ID
     */
    @JsonProperty("order_id")
    private Long orderId;
    
    /**
     * 是否为经济型商品
     */
    @JsonProperty("is_quantum")
    private Boolean isQuantum;
    
    /**
     * 最后状态变更时间范围
     */
    @JsonProperty("last_changed_status_date")
    private OzonDateRange lastChangedStatusDate;
    
    @Data
    @Builder
    public static class OzonDateRange {
        /**
         * 开始时间
         */
        @JsonProperty("from")
        private OffsetDateTime from;
        
        /**
         * 结束时间
         */
        @JsonProperty("to")
        private OffsetDateTime to;
    }
}
