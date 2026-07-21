package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存过账单分页VO
 *
 * @author erp
 */
@Data
@Schema(title = "库存过账单分页VO")
public class StockPostingPageVO {

    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "过账单号")
    private String postingNo;

    @Schema(title = "仓库ID")
    private Long warehouseId;

	@Schema(title = "仓库展示信息")
	private WarehouseDisplayVO warehouseDisplay;

	@Schema(title = "区域ID")
	private Long regionId;

	@Schema(title = "区域展示信息")
	private RegionDisplayVO regionDisplay;

	@Schema(title = "过账类型")
    private String postingType;

    @Schema(title = "过账类型描述")
    private String postingTypeDesc;

    @Schema(title = "来源单据类型")
    private String sourceType;

    @Schema(title = "来源单据ID")
    private Long sourceId;

    @Schema(title = "来源单据号")
    private String sourceNo;

    @Schema(title = "关联过账单ID")
    private Long relatedPostingId;

    @Schema(title = "业务发生时间")
    private LocalDateTime bizTime;

    @Schema(title = "过账时间")
    private LocalDateTime postTime;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建人ID")
    private Long createBy;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @Schema(title = "SKU 种类数")
    private Integer skuCount;

    @Schema(title = "总数量")
    private Integer totalQuantity;

}
