package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 出库单（下架页列表 + 详情）。status 已按平台视角映射（CONFIRMED→PENDING 待下架）。
 *
 * @author erp
 */
@Data
@Schema(title = "出库单(下架)")
public class OutboundOrderVO {

    @Schema(title = "出库单ID")
    private Long id;

    @Schema(title = "出库单号")
    private String outboundNo;

    @Schema(title = "来源类型 SALES/CUSTOM")
    private String sourceType;

    @Schema(title = "本出库单关联的销售订单数")
    private Integer salesOrderCount;

    @Schema(title = "货主ID")
    private Long erpTenantId;

    @Schema(title = "货主名称")
    private String ownerName;

    @Schema(title = "所属WMS服务商ID")
    private Long operatorId;

    @Schema(title = "所属WMS服务商名称")
    private String operatorName;

    @Schema(title = "仓库ID")
    private Long warehouseId;

    @Schema(title = "仓库名称")
    private String warehouseName;

    @Schema(title = "SKU种类数")
    private Integer skuKinds;

    @Schema(title = "总件数")
    private Integer totalQty;

    @Schema(title = "下架模式 CENTRALIZED/BY_ORDER/SECONDARY")
    private String pickMode;

    @Schema(title = "拣货员ID")
    private Long pickerId;

    @Schema(title = "拣货员姓名")
    private String pickerName;

    @Schema(title = "当前拣货任务ID")
    private Long pickTaskId;

    @Schema(title = "当前拣货任务号")
    private String pickTaskNo;

    @Schema(title = "当前拣货任务包含的出库单数")
    private Integer pickTaskOutboundOrderCount;

    @Schema(title = "当前拣货任务包含的销售订单数")
    private Integer pickTaskSalesOrderCount;

    @Schema(title = "状态 PENDING/PICKING/PICKED/BACKORDER/PACKED/SHIPPED/COMPLETED")
    private String status;

    @Schema(title = "创建时间")
    private String createTime;

    @Schema(title = "明细(详情才带)")
    private List<OutboundOrderItemVO> items;

}
