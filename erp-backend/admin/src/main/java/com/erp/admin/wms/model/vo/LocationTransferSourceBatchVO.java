package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LocationTransferSourceBatchVO {

	private Long id;

	private Long warehouseId;

	private Long wmsTenantId;

	private Long erpTenantId;

	private String ownerName;

	private String skuCode;

	private String warehouseSkuCode;

	private Integer quantity;

	private Integer reservedQty;

	private String quality;

	private LocalDate inboundDate;

	private String locationCode;

	private Long zoneId;

	private Long palletId;

	private String palletNo;

	private String palletStatus;

	private String palletType;

	private Long slotId;

	private String slotCode;

}
