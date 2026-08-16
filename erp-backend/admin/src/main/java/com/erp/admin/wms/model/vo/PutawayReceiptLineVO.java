package com.erp.admin.wms.model.vo;

import lombok.Data;

/**
 * A quantity line written by one completed putaway order.
 */
@Data
public class PutawayReceiptLineVO {

	private Long palletId;

	private Long locationId;

	private String locationCode;

	private String palletNo;

	private String slotCode;

	private String skuCode;

	private Long erpTenantId;

	private String warehouseSkuCode;

	private String quality;

	private Integer quantity;

	private String overrideReason;

}
