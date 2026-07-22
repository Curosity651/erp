package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FboInventoryPageVO {
    private Long id;
    private String platform;
    private Long shopId;
    private String shopName;
    private String platformWarehouseId;
    private String platformWarehouseName;
    private String platformItemId;
    private String skuCode;
    private SkuBriefVO skuBrief;
    private Integer quantity;
    private LocalDateTime syncedAt;
    private Boolean stale;
}

