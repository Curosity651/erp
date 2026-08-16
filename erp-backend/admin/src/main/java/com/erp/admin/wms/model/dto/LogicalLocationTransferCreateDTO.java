package com.erp.admin.wms.model.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "逻辑库位调整单创建参数")
public class LogicalLocationTransferCreateDTO {

	@NotNull(message = "仓库不能为空")
	private Long warehouseId;

	@NotNull(message = "货主不能为空")
	private Long erpTenantId;

	@NotBlank(message = "调整原因不能为空")
	private String reasonCode;

	private String reason;

	private String remark;

	@Valid
	@NotEmpty(message = "调整明细不能为空")
	private List<Item> items;

	@Data
	public static class Item {

		@NotNull(message = "源库存不能为空")
		private Long sourceInventoryId;

		@NotNull(message = "目标库位不能为空")
		private Long targetLocationId;

		@NotNull(message = "移动数量不能为空")
		@Min(value = 1, message = "移动数量必须大于0")
		private Integer quantity;

		private String remark;

	}

}
