package com.erp.admin.order.model.dto;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class SubmitFulfillmentDTO {
	@NotNull(message = "订单不能为空")
	private Long erpOrderId;
	private Long wmsWarehouseId;
}
