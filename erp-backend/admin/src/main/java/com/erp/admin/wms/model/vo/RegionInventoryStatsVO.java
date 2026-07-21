package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 区域库存统计卡片VO（全局汇总）
 */
@Data
@Schema(title = "区域库存统计")
public class RegionInventoryStatsVO {

    @Schema(title = "总区域可售")
    private Integer totalRegionAvailable;

    @Schema(title = "总区域预占")
    private Integer totalRegionReserved;

    @Schema(title = "总区域在途")
    private Integer totalRegionInTransit;

    @Schema(title = "总区域残品")
    private Integer totalRegionDamaged;

    @Schema(title = "区域数量")
    private Integer regionCount;
}
