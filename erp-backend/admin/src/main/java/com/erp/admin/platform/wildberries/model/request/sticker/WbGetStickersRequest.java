package com.erp.admin.platform.wildberries.model.request.sticker;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Wildberries 批量获取订单面单请求
 * <p>
 * 对应 API: POST /api/v3/orders/stickers
 * <p>
 * 限制：最多 100 个订单ID
 * 
 * @author system
 */
@Data
@Builder
public class WbGetStickersRequest {
    
    /** 订单ID列表（最多 100 个） */
    @JsonProperty("orders")
    private List<Long> orders;
}
