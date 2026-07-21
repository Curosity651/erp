package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 自定义出库单明细DTO（不关联电商订单，直接 SKU + 数量）
 *
 * @author erp
 */
@Data
@Schema(title = "自定义出库单明细DTO")
public class CustomOutboundItemDTO {

    @NotBlank(message = "SKU编码不能为空")
    @Schema(title = "SKU编码")
    private String skuCode;

    @NotNull(message = "出库数量不能为空")
    @Min(value = 1, message = "出库数量必须大于0")
    @Schema(title = "出库数量")
    private Integer quantity;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(title = "备注")
    private String remark;

}
