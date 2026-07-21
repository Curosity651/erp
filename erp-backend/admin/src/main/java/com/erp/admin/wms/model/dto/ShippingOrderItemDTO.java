package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 物流单明细数据传输对象
 *
 * @author erp
 */
@Data
@Schema(title = "物流单明细数据传输对象")
public class ShippingOrderItemDTO {

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

	@NotNull(message = "发货数量不能为空")
	@Min(value = 1, message = "发货数量必须大于0")
	@Schema(title = "发货数量")
	private Integer quantity;

	@Size(max = 500, message = "备注长度不能超过500")
	@Schema(title = "备注")
	private String remark;

}
