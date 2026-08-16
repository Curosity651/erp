package com.erp.admin.wms.model.dto;

import lombok.Data;

@Data
public class LocationInventoryKey {

	private Long tenantId;

	private Long wmsTenantId;

	private Long erpTenantId;

	private Long warehouseId;

	private Long locationId;

	private String skuCode;

	private String quality;

}
