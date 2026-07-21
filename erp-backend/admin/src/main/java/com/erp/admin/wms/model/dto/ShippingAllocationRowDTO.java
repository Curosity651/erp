package com.erp.admin.wms.model.dto;

import lombok.Data;

/**
 * 物流单付款/发货占用校验行。
 */
@Data
public class ShippingAllocationRowDTO {

	private Long purchaseOrderItemId;

	private String skuCode;

	private Integer totalQuantity;

	private Integer shippedQuantity;

	private Integer pendingReservedQuantity;

}
