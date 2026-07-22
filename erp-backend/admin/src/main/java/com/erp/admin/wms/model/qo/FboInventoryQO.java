package com.erp.admin.wms.model.qo;

import lombok.Data;

@Data
public class FboInventoryQO {
    private String platform;
    private Long shopId;
    private String skuCode;
    private String platformWarehouseName;
}

