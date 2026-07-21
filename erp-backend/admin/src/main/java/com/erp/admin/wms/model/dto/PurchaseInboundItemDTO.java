package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 采购入库单明细数据传输对象
 *
 * @author erp
 */
@Data
@Schema(title = "采购入库单明细数据传输对象")
public class PurchaseInboundItemDTO {

    @NotNull(message = "物流单明细ID不能为空")
    @Schema(title = "物流单明细ID")
    private Long shippingOrderItemId;

    @NotNull(message = "采购单ID不能为空")
    @Schema(title = "采购单ID")
    private Long purchaseOrderId;

    @NotNull(message = "采购单明细ID不能为空")
    @Schema(title = "采购单明细ID")
    private Long purchaseOrderItemId;

    @NotBlank(message = "SKU编码不能为空")
    @Size(max = 100, message = "SKU编码长度不能超过100")
    @Schema(title = "SKU编码")
    private String skuCode;

    @NotNull(message = "应到数量不能为空")
    @Min(value = 1, message = "应到数量必须大于0")
    @Schema(title = "应到数量")
    private Integer expectedQuantity;

    @NotNull(message = "实到数量不能为空")
    @Min(value = 0, message = "实到数量不能为负数")
    @Schema(title = "实到数量")
    private Integer actualQuantity;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(title = "备注")
    private String remark;

}
