package com.erp.admin.platform.dashboard.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 海外仓平台数据分析看板总返回（契约见前端 platform-dashboard/types.ts）。
 *
 * @author erp
 */
@Data
@Schema(title = "平台数据分析看板数据")
public class PlatformDashboardDataVO {

    private OpsOverview opsOverview;

    private Capacity capacity;

    private ThroughputTrend throughput;

    private OperatorRanking operatorRanking;

    /** A 运营总览 KPI */
    @Data
    public static class OpsOverview {
        private long onHandQty;
        private long skuCount;
        private long ownerCount;
        private long todayInboundQty;
        private long todayInboundOrders;
        private long todayOutboundQty;
        private long todayOutboundOrders;
        private Pending pending = new Pending();
    }

    /** A 待处理作业积压（单据数） */
    @Data
    public static class Pending {
        private long receiving;
        private long putaway;
        private long pickPack;
        private long outbound;
    }

    /** B 仓容利用率 */
    @Data
    public static class Capacity {
        private List<WarehouseCapacity> byWarehouse;
        private List<ZoneOccupancy> byZone;
    }

    /** B 单仓库容占用 */
    @Data
    public static class WarehouseCapacity {
        private Long warehouseId;
        private String warehouseName;
        private long used;
        private long total;
    }

    /** B 分区占用 */
    @Data
    public static class ZoneOccupancy {
        private String zone;
        private long locationCount;
        private long invQty;
    }

    /** C 吞吐趋势（度量=单据数），三数组与 dates 等长下标对齐 */
    @Data
    public static class ThroughputTrend {
        private List<String> dates;
        private List<Long> inbound;
        private List<Long> outbound;
        private List<Long> returns;
    }

    /** D WMS 服务商排名（按服务商维度聚合） */
    @Data
    public static class OperatorRanking {
        private List<OperatorRankingItem> byStock;
        private List<OperatorRankingItem> byThroughput;
    }

    /** D 服务商排名单项 */
    @Data
    public static class OperatorRankingItem {
        private Long wmsTenantId;
        private String operatorName;
        private long qty;
    }

}
