package com.erp.admin.platform.ozon.model.response.act;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ozon 运单状态查询响应：{"result": {"status": "ready"}}
 * <p>
 * 状态取值未在官方 OpenAPI 中收录，实测可见 {@code in_process} / {@code ready}，
 * 另有资料提及 {@code FORMED}。故此处不做枚举映射，由调用方按「就绪 / 出错 / 其余继续等待」
 * 三态判断，避免未知状态被误判为失败。
 *
 * @author system
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OzonActCheckStatusResponse {

    @JsonProperty("result")
    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        /** 原始状态串，如 in_process / ready / FORMED / error */
        @JsonProperty("status")
        private String status;
    }

    /** 原始状态串，缺失时返回 null */
    public String status() {
        return result != null ? result.getStatus() : null;
    }
}
