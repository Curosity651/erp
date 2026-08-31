package com.erp.admin.wms.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryMutationLine {
	private Long inventoryId;
	private Long locationId;
	private Long counterpartLocationId;
	private String skuCode;
	private String quality;
	private Integer quantityDelta;
	private Integer reservedDelta;
	private Integer beforeQuantity;
	private Integer afterQuantity;
	private Integer beforeReserved;
	private Integer afterReserved;
}
