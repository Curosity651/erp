package com.erp.admin.platform.ozon.model.request.act;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Ozon 运单(交接单 act)创建请求
 * <p>
 * API: POST /v2/posting/fbs/act/create
 * <p>
 * 运单按「物流方式 + 发货日期」维度生成，由 Ozon 汇总当日该物流方式下的全部待发货件，
 * 因此其内容不等于调用方选中的订单集合。
 *
 * @author system
 */
@Data
@Builder
public class OzonActCreateRequest {

    /**
     * 物流方式 ID（来自 posting.delivery_method.id）
     */
    @JsonProperty("delivery_method_id")
    private Long deliveryMethodId;

    /**
     * 发货日期，ISO-8601，如 2026-07-09T00:00:00Z
     */
    @JsonProperty("departure_date")
    private String departureDate;

    /**
     * container 数量（箱数），当前固定 1
     */
    @JsonProperty("containers_count")
    private Integer containersCount;
}
