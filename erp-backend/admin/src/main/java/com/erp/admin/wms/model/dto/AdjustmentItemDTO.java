package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 调整单明细数据传输对象
 * 方向由调整类型决定，无需用户选择
 *
 * @author erp
 */
@Data
@Schema(title = "调整单明细数据传输对象")
public class AdjustmentItemDTO {

	@NotNull(message = "目标批次不能为空")
	@Schema(title = "目标批次ID（wms_physical_inventory.id）——报废锁定到具体批次/库位")
	private Long physicalInventoryId;

	@NotBlank(message = "SKU编码不能为空")
	@Size(max = 100, message = "SKU编码长度不能超过100")
	@Schema(title = "SKU编码")
	private String skuCode;

	@NotNull(message = "调整数量不能为空")
	@Min(value = 1, message = "调整数量必须大于0")
	@Schema(title = "调整数量（始终为正数）")
	private Integer quantity;

	@Size(max = 500, message = "备注长度不能超过500")
	@Schema(title = "备注")
	private String remark;

}
