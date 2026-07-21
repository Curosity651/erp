package com.erp.admin.platform.wildberries.model.response.supply;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wildberries Supply 条码响应
 * <p>
 * 对应 API: GET /api/v3/supplies/{supplyId}/barcode
 * <p>
 * 仅在 Supply 转移到配送后可用
 * 
 * @author system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WbSupplyBarcodeResponse {
    
    /** 条码内容（Supply ID） */
    @JsonProperty("barcode")
    private String barcode;
    
    /** Base64 编码的条码图片 */
    @JsonProperty("file")
    private String file;
}
