package com.erp.admin.wms.model.vo;

import java.util.List;

import lombok.Data;

@Data
public class LocationInventoryDetailVO {

	private LocationInventoryGridVO location;

	private List<InventoryLineVO> items;

	@Data
	public static class InventoryLineVO {

		private Long inventoryId;

		private Long wmsTenantId;

		private String wmsTenantName;

		private Long erpTenantId;

		private String ownerName;

		private String skuCode;

		private String skuName;

		private String quality;

		private Integer quantity;

		private Integer reservedQuantity;

		private Integer availableQuantity;

		private Integer outerLengthMm;

		private Integer outerWidthMm;

		private Integer outerHeightMm;

		private Integer outerGrossWeightG;

	}

}
