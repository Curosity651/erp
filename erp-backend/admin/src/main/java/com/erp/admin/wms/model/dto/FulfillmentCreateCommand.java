package com.erp.admin.wms.model.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class FulfillmentCreateCommand {
	private Long tenantId;
	private Long wmsTenantId;
	private Long erpTenantId;
	private Long warehouseId;
	private Long shopId;
	private Long logisticsProductId;
	private String logisticsProductCode;
	private String logisticsProductName;
	private String logisticsProductDescription;
	private BigDecimal logisticsProductDefaultFee;
	private BigDecimal logisticsProductActualFee;
	private String logisticsProductCurrency;
	private String sourceType;
	private Long sourceOrderId;
	private String sourceOrderNo;
	private String platformStatus;
	private String recipientName;
	private String recipientPhone;
	private String recipientAddress;
	private List<Item> items;

	@Data
	public static class Item {
		private String skuCode;
		private String warehouseSkuCode;
		private String skuName;
		private String imageUrl;
		private String quality = "GOOD";
		private Integer quantity;
		private Integer outerLengthMm;
		private Integer outerWidthMm;
		private Integer outerHeightMm;
		private Integer outerGrossWeightG;
	}
}
