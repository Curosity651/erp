package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 可入库物流单视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "可入库物流单视图对象")
public class AvailableShippingVO {

    @Schema(title = "物流单ID")
    private Long id;

    @Schema(title = "物流单号")
    private String shippingNo;

    @Schema(title = "物流商ID")
    private Long providerId;

    @Schema(title = "物流商名称")
    private String providerName;

    @Schema(title = "发货日期")
    private LocalDate shippingDate;

    @Schema(title = "发货总数")
    private Integer totalQuantity;

    @Schema(title = "已入库数量")
    private Integer receivedQuantity;

    @Schema(title = "待入库数量（发货总数 - 已入库数量）")
    private Integer pendingQuantity;

    @Schema(title = "目标区域ID")
    private Long targetRegionId;

    @Schema(title = "目标区域")
    private RegionDisplayVO targetRegion;

}
