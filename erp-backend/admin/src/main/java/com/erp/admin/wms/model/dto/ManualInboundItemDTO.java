package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 自定义入库单明细数据传输对象（无采购/物流关联）
 *
 * @author erp
 */
@Data
@Schema(title = "自定义入库单明细数据传输对象")
public class ManualInboundItemDTO {

    @NotBlank(message = "SKU编码不能为空")
    @Size(max = 100, message = "SKU编码长度不能超过100")
    @Schema(title = "SKU编码")
    private String skuCode;

    @NotNull(message = "应到数量不能为空")
    @Min(value = 1, message = "应到数量必须大于0")
    @Schema(title = "应到数量")
    private Integer expectedQuantity;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(title = "备注")
    private String remark;

}
