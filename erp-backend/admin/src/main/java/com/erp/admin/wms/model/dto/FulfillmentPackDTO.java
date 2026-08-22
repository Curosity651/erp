package com.erp.admin.wms.model.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FulfillmentPackDTO {
	private String carrierCode;
	@NotBlank(message = "承运商不能为空")
	private String carrierName;
	@NotBlank(message = "运输方式不能为空")
	private String shippingMethod;
	@NotBlank(message = "跟踪号不能为空")
	private String trackingNo;
	@NotNull(message = "包裹重量不能为空")
	@DecimalMin(value = "0", inclusive = false, message = "包裹重量必须大于0")
	private BigDecimal packageWeightKg;
}
