package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(title = "拣货任务关联出库单")
public class PickTaskOutboundVO {

    @Schema(title = "出库单ID")
    private Long outboundOrderId;

    @Schema(title = "出库单号")
    private String outboundNo;

    @Schema(title = "关联销售订单数")
    private Integer salesOrderCount;

    @Schema(title = "SKU种类数")
    private Integer skuCount;

    @Schema(title = "总件数")
    private Integer totalQuantity;

    @Schema(title = "二次分拣周转筐号")
    private String toteNo;

    @Schema(title = "是否需要按出库单分货")
    private Boolean sortRequired;

	@Schema(title = "平台订单级分货包裹")
	private List<OutboundPackageVO> packages;
}
