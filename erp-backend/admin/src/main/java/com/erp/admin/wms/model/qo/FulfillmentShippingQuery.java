package com.erp.admin.wms.model.qo;

import java.time.LocalDate;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class FulfillmentShippingQuery {
	private Long erpTenantId;
	private Long warehouseId;
	private String fulfillmentStatus;
	private Long shippedBy;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate startDate;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate endDate;
}
