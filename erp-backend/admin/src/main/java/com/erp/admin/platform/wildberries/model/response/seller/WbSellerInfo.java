package com.erp.admin.platform.wildberries.model.response.seller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wildberries 卖家信息响应
 * <p>
 * API: GET /api/v1/seller-info
 * 
 * @author system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WbSellerInfo {
    
    /**
     * 卖家名称
     * 例如："ИП Кружинин В. Р."
     */
    private String name;
    
    /**
     * Wildberries 卖家 ID (UUID 格式)
     * 例如："e8923014-e233-47q8-898e-3cc86d67ea61"
     */
    private String sid;
    
    /**
     * 卖家商标/品牌名称
     * 例如："Flax Store"
     */
    private String tradeMark;
}
