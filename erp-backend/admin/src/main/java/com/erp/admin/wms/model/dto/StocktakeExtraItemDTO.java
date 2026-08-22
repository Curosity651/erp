package com.erp.admin.wms.model.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StocktakeExtraItemDTO {

	@NotNull(message = "库位任务不能为空")
	private Long taskId;

	@NotNull(message = "货主不能为空")
	private Long erpTenantId;

	@javax.validation.constraints.NotBlank(message = "SKU不能为空")
	private String skuCode;

	@NotNull(message = "实盘数量不能为空")
	@Min(value = 1, message = "实盘数量必须大于0")
	private Integer actualQuantity;

	private String slotCode;

	private Long palletId;

	private String quality = "GOOD";

	@DecimalMin(value = "0.01", message = "托盘利用率必须大于0")
	@DecimalMax(value = "100", message = "托盘利用率不能超过100")
	private BigDecimal capacityPercent;

	private Boolean manualFull;

	private LocalDate inboundDate;

	private String remark;
}
