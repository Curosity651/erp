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

    @Schema(title = "SKU编码")
    private String skuCode;

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

}
