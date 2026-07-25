package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 拣货单（PICKING 后查看）。
 *
 * @author erp
 */
@Data
@Schema(title = "拣货单")
public class PickListVO {

    @Schema(title = "出库单号")
    private String outboundNo;

    @Schema(title = "来源类型 SALES/CUSTOM")
    private String sourceType;

    @Schema(title = "货主名称")
    private String ownerName;

    @Schema(title = "仓库名称")
    private String warehouseName;

    @Schema(title = "下架模式")
    private String pickMode;

    @Schema(title = "拣货员姓名")
    private String pickerName;

    @Schema(title = "拣货任务ID")
    private Long taskId;

    @Schema(title = "拣货任务号")
    private String taskNo;

    @Schema(title = "任务类型 SINGLE/WAVE")
    private String taskType;

    @Schema(title = "任务状态 PICKING/EXCEPTION/SORTING/COMPLETED/CANCELLED")
    private String taskStatus;

    @Schema(title = "任务关联的出库单数")
    private Integer outboundOrderCount;

    @Schema(title = "任务关联的销售订单数")
    private Integer salesOrderCount;

    @Schema(title = "需要二次分拣的订单数")
    private Integer secondaryOrderCount;

    @Schema(title = "当前是否允许确认拣货完成")
    private Boolean completable;

    @Schema(title = "计划拣货总数")
    private Integer plannedQuantity;

    @Schema(title = "已拣总数")
    private Integer pickedQuantity;

    @Schema(title = "任务异常原因")
    private String exceptionReason;

    @Schema(title = "任务关联的出库单")
    private List<PickTaskOutboundVO> outboundOrders;

    @Schema(title = "取货清单(按库位排序)")
    private List<PickAllocationVO> allocations;

}
