package com.erp.admin.wms.model.dto;

import lombok.Data;

@Data
public class InventoryReservationRequest {

	private Long fulfillmentOrderId;

	private Long fulfillmentItemId;

	private Long tenantId;

	private Long wmsTenantId;

	private Long erpTenantId;

	private Long warehouseId;

	private String skuCode;

	private String quality;

	private Integer quantity;

}
