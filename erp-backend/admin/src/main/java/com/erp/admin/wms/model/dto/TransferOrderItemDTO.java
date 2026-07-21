package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 调拨单明细数据传输对象
 *
 * @author erp
 */
@Data
@Schema(title = "调拨单明细数据传输对象")
public class TransferOrderItemDTO {

	@Schema(title = "SKU编码（可空，缺省从源批次带出）")
	@Size(max = 100, message = "SKU编码长度不能超过100")
	private String skuCode;

	@NotNull(message = "源批次不能为空")
	@Schema(title = "源批次ID(A仓，发出时扣此批次)")
	private Long sourcePhysicalInventoryId;

	@NotBlank(message = "目标库位不能为空")
	@Size(max = 64, message = "目标库位长度不能超过64")
	@Schema(title = "目标库位编码(B仓，须落在该货主服务商租用排)")
	private String targetLocationCode;

	@NotNull(message = "调拨数量不能为空")
	@Min(value = 1, message = "调拨数量必须大于0")
	@Schema(title = "调拨数量")
	private Integer quantity;

	@Size(max = 500, message = "备注长度不能超过500")
	@Schema(title = "备注")
	private String remark;

}
