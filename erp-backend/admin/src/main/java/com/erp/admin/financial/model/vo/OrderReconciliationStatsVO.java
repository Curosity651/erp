package com.erp.admin.financial.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单财务对账统计 VO
 *
 * @author system
 */
@Data
@Schema(title = "订单财务对账统计")
public class OrderReconciliationStatsVO {

    @Schema(title = "订单总数")
    private Integer totalOrders;

    @Schema(title = "待履约订单数（尚未发货）")
    private Integer pendingCount;

    @Schema(title = "运输中订单数（已发货未交付）")
    private Integer inTransitCount;

    @Schema(title = "已取消订单数（发货前取消）")
    private Integer canceledCount;

    @Schema(title = "已对账订单数")
    private Integer matchedCount;

    @Schema(title = "异常订单数（应有财务记录但缺失）")
    private Integer anomalyCount;

    /**
     * 创建空的统计对象
     */
    public static OrderReconciliationStatsVO empty() {
        OrderReconciliationStatsVO vo = new OrderReconciliationStatsVO();
        vo.setTotalOrders(0);
        vo.setPendingCount(0);
        vo.setInTransitCount(0);
        vo.setCanceledCount(0);
        vo.setMatchedCount(0);
        vo.setAnomalyCount(0);
        return vo;
    }
}
