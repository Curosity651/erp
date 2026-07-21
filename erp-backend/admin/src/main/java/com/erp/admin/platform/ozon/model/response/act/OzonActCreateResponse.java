package com.erp.admin.platform.ozon.model.response.act;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ozon 运单创建响应：{"result": {"id": 84392011}}
 *
 * @author system
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OzonActCreateResponse {

    @JsonProperty("result")
    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        /** Ozon 侧运单 ID */
        @JsonProperty("id")
        private Long id;
    }

    /** 便捷取值，缺失时返回 null */
    public Long actId() {
        return result != null ? result.getId() : null;
    }
}
