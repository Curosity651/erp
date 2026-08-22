package com.erp.admin.wms.model.dto;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class FulfillmentLogisticsFeeDTO {

	@NotNull(message = "物流产品费用不能为空")
	@DecimalMin(value = "0", message = "物流产品费用不能小于0")
	private BigDecimal amount;

	private String adjustmentReason;

}
