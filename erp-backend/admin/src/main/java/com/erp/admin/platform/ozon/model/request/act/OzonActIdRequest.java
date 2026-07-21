package com.erp.admin.platform.ozon.model.request.act;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Ozon 运单按 ID 查询请求，供 check-status 与 get-pdf 共用（两者请求体均为 {"id": actId}）
 * <p>
 * API: POST /v2/posting/fbs/act/check-status
 * API: POST /v2/posting/fbs/act/get-pdf
 *
 * @author system
 */
@Data
@Builder
public class OzonActIdRequest {

    /**
     * Ozon 侧运单 ID（act/create 返回的 result.id）
     */
    @JsonProperty("id")
    private Long id;
}
