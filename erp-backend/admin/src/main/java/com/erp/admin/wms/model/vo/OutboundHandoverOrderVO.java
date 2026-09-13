package com.erp.admin.wms.model.vo;

import com.erp.admin.wms.model.entity.WmsOutboundHandoverOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OutboundHandoverOrderVO extends WmsOutboundHandoverOrder {
	private Long erpTenantId;
	private String ownerName;
	private Long warehouseId;
	private String warehouseName;
	private String fulfillmentNo;
	private String sourceType;
	private String sourceOrderNo;
	private String recipientName;
	private String recipientAddress;
	private String carrierName;
	private String shippingMethod;
	private String trackingNo;
	private String handoverByName;
}
