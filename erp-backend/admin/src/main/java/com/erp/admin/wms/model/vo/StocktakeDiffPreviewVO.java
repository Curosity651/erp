package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 盘点差异预览VO
 *
 * @author erp
 */
@Data
@Schema(description = "盘点差异预览")
public class StocktakeDiffPreviewVO {

    @Schema(description = "盘点明细总数")
    private Integer totalCount;

    @Schema(description = "无差异数量")
    private Integer noDiffCount;

    @Schema(description = "盘盈SKU数量（正差异）")
    private Integer profitCount;

    @Schema(description = "盘盈总数量")
    private Integer profitQuantity;

    @Schema(description = "盘亏SKU数量（负差异）")
    private Integer lossCount;

    @Schema(description = "盘亏总数量")
    private Integer lossQuantity;

    @Schema(description = "未盘点数量")
    private Integer pendingCount;

    @Schema(description = "差异明细列表（仅包含有差异的项）")
    private List<StocktakeDiffItemVO> diffItems;
}
