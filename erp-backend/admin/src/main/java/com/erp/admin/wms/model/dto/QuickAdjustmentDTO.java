package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 快速库存调整DTO
 * 方向由调整类型决定，无需用户选择
 *
 * @author erp
 */
@Data
@Schema(title = "快速库存调整DTO")
public class QuickAdjustmentDTO {

    @Schema(description = "仓库ID")
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    @Schema(description = "SKU编码")
    @NotBlank(message = "SKU编码不能为空")
    @Size(max = 100, message = "SKU编码长度不能超过100")
    private String skuCode;

    @Schema(description = "调整类型: SCRAP-报废 / OFFLINE_SALE-线下销售 / OTHER_OUT-其他出库 / OFFLINE_PURCHASE-线下采购 / OTHER_IN-其他入库")
    @NotNull(message = "调整类型不能为空")
    private String adjustmentType;

    @Schema(description = "调整数量（始终为正数）")
    @NotNull(message = "调整数量不能为空")
    @Min(value = 1, message = "调整数量必须大于0")
    private Integer quantity;

    @Schema(description = "调整原因")
    private String adjustmentReason;
}
