package com.erp.admin.wms.model.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class StocktakeExtraItemDTO {

	@NotNull(message = "库位任务不能为空")
	private Long taskId;

	@NotNull(message = "货主不能为空")
	private Long erpTenantId;

	@NotBlank(message = "SKU不能为空")
	private String skuCode;

	@NotNull(message = "实盘数量不能为空")
	@Min(value = 1, message = "实盘数量必须大于0")
	private Integer actualQuantity;

	private String quality = "GOOD";

	private LocalDate inboundDate;

	private String remark;
}

