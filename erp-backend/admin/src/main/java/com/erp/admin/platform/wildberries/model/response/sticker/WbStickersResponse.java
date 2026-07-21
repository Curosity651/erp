package com.erp.admin.platform.wildberries.model.response.sticker;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Wildberries 批量订单面单响应
 * <p>
 * 对应 API: POST /api/v3/orders/stickers
 * 
 * @author system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WbStickersResponse {
    
    /** 面单列表 */
    @JsonProperty("stickers")
    @Builder.Default
    private List<WbSticker> stickers = new ArrayList<>();
}
