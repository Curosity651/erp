package com.erp.admin.platform.ozon.model.request.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Ozon FBO 订单过滤条件
 * <p>
 * 用于 FBO 订单列表查询的过滤参数
 *
 * @author system
 */
@Data
@Builder
public class OzonFboPostingFilter {

    /**
     * 开始时间（ISO 8601 格式）
     * 例如：2023-01-01T00:00:00Z
     */
    @JsonProperty("since")
    private String since;

    /**
     * 结束时间（ISO 8601 格式）
     * 例如：2023-01-31T23:59:59Z
     */
    @JsonProperty("to")
    private String to;

    /**
     * 状态过滤
     * 可选值：awaiting_packaging, awaiting_deliver, delivering, delivered, cancelled
     */
    @JsonProperty("status")
    private String status;

    /**
     * 仓库 ID 列表
     */
    @JsonProperty("warehouse_id")
    private List<Long> warehouseId;
}
