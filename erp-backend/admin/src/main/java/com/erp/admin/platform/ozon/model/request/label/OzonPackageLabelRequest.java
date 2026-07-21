package com.erp.admin.platform.ozon.model.request.label;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Ozon 面单请求模型
 * 
 * @author system
 */
@Data
@Builder
public class OzonPackageLabelRequest {
    
    /**
     * 发货单号列表
     * 最多 20 个
     */
    @JsonProperty("posting_number")
    private List<String> postingNumber;
}
