package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class PickTaskPreviewVO {
    private Long warehouseId;
    private String warehouseName;
    private Long erpTenantId;
    private String ownerName;
    private String sourceType;
    private String taskType;
    private List<Long> outboundOrderIds;
    /** 出库单数，不是电商平台订单数。 */
    private Integer orderCount;
    private Integer salesOrderCount;
    private Integer skuCount;
    private Integer totalQuantity;
    private Integer wholePalletCount;
    private Integer secondaryOrderCount;
}
