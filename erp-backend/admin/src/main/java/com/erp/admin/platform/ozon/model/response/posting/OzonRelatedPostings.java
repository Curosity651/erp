package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * Ozon 关联订单信息模型
 * 
 * @author system
 */
@Data
public class OzonRelatedPostings {
    
    /**
     * 关联的发货单号列表
     */
    @JsonProperty("related_posting_numbers")
    private List<String> relatedPostingNumbers;
}
