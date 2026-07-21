package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(title = "库存预测详情VO（区域维度）")
public class ForecastDetailVO {

    @Schema(title = "SKU简要信息")
    private SkuBriefVO skuBrief;

    @Schema(title = "区域ID")
    private Long regionId;

    @Schema(title = "区域名称")
    private String regionName;

    @Schema(title = "当前库存")
    private CurrentStockVO currentStock;

    @Schema(title = "安全库存")
    private Integer safetyStock;

    @Schema(title = "日均销量")
    private Integer dailySales;

    @Schema(title = "可售天数")
    private Integer sellableDays;

    @Schema(title = "库存状态")
    private String status;

    @Schema(title = "预警阈值天数")
    private Integer thresholdDays;

    @Schema(title = "每日预测列表")
    private List<ForecastDayVO> forecastList;

    @Schema(title = "入库来源", description = "跨仓汇总，保留仓库级明细")
    private List<IncomingSourceVO> incomingSources;

    @Schema(title = "有效安全库存", description = "max(safetyStock, dailySales × thresholdDays)")
    private Integer effectiveSafetyStock;

    @Schema(title = "待发货总量", description = "不含在预测中的待发货库存")
    private Integer pendingShipmentTotal;

    // ========== 内部类 ==========

    @Data
    @Schema(title = "当前库存信息")
    public static class CurrentStockVO {
        @Schema(title = "区域可用", description = "SUM(区域内自有仓.available)")
        private Integer available;

        @Schema(title = "区域预占", description = "region_inventory.reserved")
        private Integer reserved;

        @Schema(title = "区域可售", description = "available - reserved")
        private Integer sellable;

        @Schema(title = "区域在途", description = "SUM(区域内自有仓.in_transit)")
        private Integer inTransit;

        @Schema(title = "待发货", description = "区域内各仓汇总")
        private Integer pendingShipment;
    }

    @Data
    @Schema(title = "每日预测")
    public static class ForecastDayVO {
        @Schema(title = "日期")
        private LocalDate date;

        @Schema(title = "期初库存")
        private Integer openingStock;

        @Schema(title = "当日入库")
        private Integer incoming;

        @Schema(title = "当日销量")
        private Integer sales;

        @Schema(title = "期末库存")
        private Integer closingStock;

        @Schema(title = "库存状态")
        private String status;

        @Schema(title = "入库明细")
        private List<IncomingDetailVO> incomingDetails;
    }

    @Data
    @Schema(title = "入库明细")
    public static class IncomingDetailVO {
        @Schema(title = "类型", description = "IN_TRANSIT / PENDING_SHIPMENT")
        private String type;

        @Schema(title = "来源单号")
        private String sourceNo;

        @Schema(title = "数量")
        private Integer quantity;

        @Schema(title = "状态")
        private String status;
    }

    @Data
    @Schema(title = "入库来源")
    public static class IncomingSourceVO {
        @Schema(title = "类型", description = "LOGISTICS")
        private String type;

        @Schema(title = "单号")
        private String sourceNo;

        @Schema(title = "SKU编码")
        private String skuCode;

        @Schema(title = "数量")
        private Integer quantity;

        @Schema(title = "预计到达日期")
        private LocalDate expectedDate;
    }
}
