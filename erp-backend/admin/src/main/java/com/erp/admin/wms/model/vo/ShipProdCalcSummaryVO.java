package com.erp.admin.wms.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 发货生产测算汇总返回（状态卡计数 + 分页行）。
 *
 * @author erp
 */
@Data
@Builder
public class ShipProdCalcSummaryVO {

    /** 过滤后总行数（分页用） */
    private long total;

    private Counts counts;

    private int shipThresholdDays;
    private int prodThresholdDays;
    private int safetyStockDays;

    private List<ShipProdCalcRowVO> list;

    /**
     * 状态卡计数（基于全量、未分页结果）。
     */
    @Data
    @Builder
    public static class Counts {
        /** 需发货 */
        private int needShip;
        /** 需订货 */
        private int needProduce;
        /** 断档预警 */
        private int shortage;
        /** 待观察（零销量/新品无估算） */
        private int noSales;
    }
}
