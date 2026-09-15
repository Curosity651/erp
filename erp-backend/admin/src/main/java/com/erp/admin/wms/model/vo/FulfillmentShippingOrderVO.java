package com.erp.admin.wms.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class FulfillmentShippingOrderVO {
	private Long id;
	private Long erpTenantId;
	private String ownerName;
	private Long warehouseId;
	private String warehouseName;
	private String fulfillmentNo;
	private String sourceType;
	private String sourceOrderNo;
	private String fulfillmentStatus;
	private String recipientName;
	private String logisticsProductName;
	private BigDecimal logisticsProductActualFee;
	private String logisticsProductCurrency;
	private String carrierName;
	private String shippingMethod;
	private String trackingNo;
	private BigDecimal packageWeightKg;
	private Long shippedBy;
	private String shippedByName;
	private LocalDateTime shippedTime;
	private LocalDateTime createTime;
}
