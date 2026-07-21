package com.erp.admin.platform.wildberries.model.response.sticker;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wildberries 订单面单详情
 * <p>
 * 对应 API: POST /api/v3/orders/stickers 的响应项
 * 
 * @author system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WbSticker {
    
    /** 订单ID */
    @JsonProperty("orderId")
    private Long orderId;
    
    /** 面单标识第一部分（用于打印签名） */
    @JsonProperty("partA")
    private String partA;
    
    /** 面单标识第二部分（用于打印签名） */
    @JsonProperty("partB")
    private String partB;
    
    /** 面单编码值 */
    @JsonProperty("barcode")
    private String barcode;
    
    /** Base64 编码的面单图片 */
    @JsonProperty("file")
    private String file;
}
