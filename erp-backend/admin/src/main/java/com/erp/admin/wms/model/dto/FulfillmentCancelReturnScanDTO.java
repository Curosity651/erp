package com.erp.admin.wms.model.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FulfillmentCancelReturnScanDTO {
	@NotNull private Long fulfillmentOrderId;
	@NotBlank private String locationCode;
	@NotBlank private String warehouseSkuCode;
	@NotNull @Min(1) private Integer quantity;
}
