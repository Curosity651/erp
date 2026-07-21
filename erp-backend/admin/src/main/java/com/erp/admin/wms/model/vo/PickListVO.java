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

    @Schema(title = "货主名称")
    private String ownerName;

    @Schema(title = "仓库名称")
    private String warehouseName;

    @Schema(title = "下架模式")
    private String pickMode;

    @Schema(title = "拣货员姓名")
    private String pickerName;

    @Schema(title = "取货清单(按库位排序)")
    private List<PickAllocationVO> allocations;

}
