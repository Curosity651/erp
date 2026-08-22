package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * FIFO 分配行（下架预览 / 拣货单明细）。
 *
 * @author erp
 */
@Data
@Schema(title = "FIFO分配行")
public class PickAllocationVO {

    @Schema(title = "拣货任务明细ID")
    private Long lineId;

    @Schema(title = "物理库存批次ID")
    private Long physicalInventoryId;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "海外仓内部SKU")
    private String warehouseSkuCode;

    @Schema(title = "SKU名称")
    private String skuName;

    @Schema(title = "库位编码")
    private String locationCode;

    @Schema(title = "批次号(inbound_date + pick_order)")
    private String batchNo;

    @Schema(title = "入库日")
    private String inboundDate;

    @Schema(title = "从该批次取货数")
    private Integer takeQty;

    @Schema(title = "计划拣货数量")
    private Integer plannedQty;

    @Schema(title = "已拣数量")
    private Integer pickedQty;

    @Schema(title = "剩余数量")
    private Integer remainingQty;

    @Schema(title = "已报告缺货数量")
    private Integer shortageQty;

    @Schema(title = "应返库数量")
    private Integer returnRequiredQty;

    @Schema(title = "已返库数量")
    private Integer returnedQty;

    @Schema(title = "明细状态 PENDING/IN_PROGRESS/COMPLETED/EXCEPTION")
    private String lineStatus;

    @Schema(title = "异常原因")
    private String exceptionReason;

    @Schema(title = "托盘ID")
    private Long palletId;

    @Schema(title = "托盘号")
    private String palletNo;

    @Schema(title = "三层层位编码")
    private String slotCode;

    @Schema(title = "拣货策略 WHOLE_PALLET/PIECE")
    private String pickStrategy;

}
