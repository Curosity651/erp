package com.erp.admin.wms.model.vo;

import lombok.Data;

@Data
public class LogicalTransferSourceVO {

	private Long inventoryId;
	private Long warehouseId;
	private Long wmsTenantId;
	private Long erpTenantId;
	private String ownerName;
	private String skuCode;
	private String warehouseSkuCode;
	private String quality;
	private Integer quantity;
	private Integer reservedQuantity;
	private Integer availableQuantity;
	private Long locationId;
	private String locationCode;
	private String rackNo;
	private String locationType;
	private String zoneName;
	private String zoneType;

}
