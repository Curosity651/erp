package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Ozon FBO 订单列表响应模型
 * <p>
 * API: POST /v2/posting/fbo/list
 * <p>
 * 注意: FBO API 的 result 字段直接是数组，与 FBS API 不同
 * <p>
 * 响应示例:
 * <pre>
 * {
 *   "result": [
 *     {
 *       "order_id": 354680487,
 *       "order_number": "16965409-0014",
 *       ...
 *     }
 *   ]
 * }
 * </pre>
 *
 * @author system
 */
@Data
public class OzonFboPostingListResponse {

    /**
     * FBO 订单列表
     * <p>
     * 注意: result 字段直接是订单数组，不是包含 postings 和 has_next 的对象
     */
    @JsonProperty("result")
    private List<OzonFboPosting> result;
}
