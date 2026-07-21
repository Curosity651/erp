package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 区域库存汇总VO
 */
@Data
@Schema(title = "区域库存汇总")
public class RegionSummaryVO {

    @Schema(title = "区域ID")
    private Long regionId;

    @Schema(title = "区域编码")
    private String regionCode;

    @Schema(title = "区域名称")
    private String regionName;

    @Schema(title = "自有仓数量")
    private Integer ownWarehouseCount;

    @Schema(title = "区域可售库存 = SUM(自有仓.available) - region.reserved")
    private Integer regionAvailable;

    @Schema(title = "区域预占库存")
    private Integer regionReserved;

    @Schema(title = "区域在途库存")
    private Integer regionInTransit;

    @Schema(title = "区域残品库存")
    private Integer regionDamaged;
}
