package com.erp.admin.wms.model.vo;

import lombok.Data;

/**
 * 物流单发货数量统计VO
 *
 * @author erp
 */
@Data
public class ShippingQuantityStatsVO {

    /**
     * 物流单ID（批量查询时使用）
     */
    private Long shippingOrderId;

    /**
     * 发货总数
     */
    private Integer totalQuantity;

    /**
     * 已入库数量
     */
    private Integer receivedQuantity;

    /**
     * Remaining quantity that can be assigned to a new inbound order.
     */
    private Integer pendingInboundQuantity;

}
