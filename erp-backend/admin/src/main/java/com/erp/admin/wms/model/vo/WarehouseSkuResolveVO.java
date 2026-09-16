package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "退货全局SKU解析结果")
public class WarehouseSkuResolveVO {

    private String requestedCode;

    private String warehouseSkuCode;

    private Long erpTenantId;

    private String ownerName;

    private String skuCode;

    private String skuName;

    private boolean matched;

    private String error;
}
