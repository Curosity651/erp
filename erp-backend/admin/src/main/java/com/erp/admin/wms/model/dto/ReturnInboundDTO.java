package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 退货入库单DTO
 * 简化版：单 SKU 场景，无 items 数组
 *
 * @author erp
 */
@Data
@Schema(title = "退货入库单DTO")
public class ReturnInboundDTO {

    @NotNull(message = "订单商品明细ID不能为空")
    @Schema(title = "订单商品明细ID (erp_order_item.id)")
    private Long orderItemId;

    @NotNull(message = "入库仓库不能为空")
    @Schema(title = "入库仓库ID")
    private Long warehouseId;

    @NotNull(message = "退货日期不能为空")
    @Schema(title = "退货日期")
    private LocalDate returnDate;

    @NotNull(message = "退货原因不能为空")
    @Schema(title = "退货原因")
    private String returnReason;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(title = "备注")
    private String remark;

    @NotNull(message = "退货数量不能为空")
    @Min(value = 1, message = "退货数量至少为1")
    @Schema(title = "退货数量")
    private Integer quantity;

}
