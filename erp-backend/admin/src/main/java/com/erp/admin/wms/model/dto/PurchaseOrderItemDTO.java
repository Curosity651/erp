package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 采购单明细数据传输对象
 *
 * @author erp
 */
@Data
@Schema(title = "采购单明细数据传输对象")
public class PurchaseOrderItemDTO {

	@Schema(title = "明细ID（编辑时传入）")
	private Long id;

	@NotBlank(message = "SKU编码不能为空")
	@Size(max = 100, message = "SKU编码长度不能超过100个字符")
	@Schema(title = "SKU编码")
	private String skuCode;

	@NotNull(message = "采购数量不能为空")
	@Min(value = 1, message = "采购数量必须大于0")
	@Schema(title = "采购数量")
	private Integer quantity;

	@NotNull(message = "单价不能为空")
	@DecimalMin(value = "0.01", message = "单价必须大于0")
	@Schema(title = "单价")
	private BigDecimal unitPrice;

	@Size(max = 500, message = "备注长度不能超过500个字符")
	@Schema(title = "备注")
	private String remark;

}
