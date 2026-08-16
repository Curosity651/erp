package com.erp.admin.wms.model.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import lombok.Data;

@Data
public class FulfillmentPackDTO {
	private String carrierCode;
	private String carrierName;
	private String trackingNo;
	@DecimalMin(value = "0", inclusive = false, message = "包裹重量必须大于0")
	private BigDecimal packageWeightKg;
}
