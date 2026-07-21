package com.erp.admin.platform.ozon.model.request.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Ozon 发货请求模型
 * 
 * @author system
 */
@Data
@Builder
public class OzonShipRequest {
    
    /**
     * 发货单号
     */
    @JsonProperty("posting_number")
    private String postingNumber;
    
    /**
     * 包裹列表
     * 每个包裹包含一组产品
     */
    @JsonProperty("packages")
    private List<OzonPackage> packages;
    
    /**
     * 附加数据选项
     */
    @JsonProperty("with")
    private OzonShipWith with;
    
    @Data
    @Builder
    public static class OzonShipWith {
        /**
         * 是否返回附加数据
         */
        @JsonProperty("additional_data")
        private Boolean additionalData;
    }
}
