package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import com.erp.admin.wms.model.enums.ForecastStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(title = "库存预测汇总VO（区域维度）")
public class ForecastSummaryVO {

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "SKU简要信息")
    private SkuBriefVO skuBrief;

    @Schema(title = "区域ID")
    private Long regionId;

    @Schema(title = "区域名称")
    private String regionName;

    @Schema(title = "区域可用库存", description = "SUM(区域内自有仓.available)")
    private Integer availableQuantity;

    @Schema(title = "区域预占库存")
    private Integer reservedQuantity;

    @Schema(title = "区域可售库存", description = "available - reserved")
    private Integer sellableQuantity;

    @Schema(title = "区域在途库存", description = "SUM(区域内自有仓.in_transit)")
    private Integer inTransitQuantity;

    @Schema(title = "待发货库存")
    private Integer pendingShipmentQuantity;

    @Schema(title = "日均销量")
    private Integer dailySales;

    @Schema(title = "可售天数")
    private Integer sellableDays;

    @Schema(title = "预计断货日期")
    private LocalDate stockoutDate;

    @Schema(title = "库存状态")
    private ForecastStatus status;

    @Schema(title = "有效安全库存", description = "max(safetyStock, dailySales × thresholdDays)")
    private Integer effectiveSafetyStock;

}
