package com.erp.admin.wms.model.vo;

import lombok.Data;

/** Authoritative order item snapshot used while creating a linked return. */
@Data
public class ReturnOrderSourceVO {

    private Long orderId;
    private Long orderItemId;
    private Long erpTenantId;
    private String platformOrderId;
    private String platform;
    private String outboundStatus;
    private String skuCode;
    private Integer shippedQuantity;
    private Integer returnedQuantity;

}
