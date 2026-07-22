package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FboInventorySummaryVO {
    private Integer skuCount;
    private Integer shopCount;
    private Integer totalQuantity;
    private LocalDateTime lastSyncedAt;
    private Boolean stale;
}

