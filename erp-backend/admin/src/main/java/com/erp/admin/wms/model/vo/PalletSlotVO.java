package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PalletSlotVO {
    private Long slotId;
    private Long locationId;
    private String locationCode;
    private String slotCode;
    private String rackNo;
    private Integer columnNo;
    private Integer levelNo;
    private Long zoneId;
    private String zoneName;
    private String zoneType;
    private String slotStatus;
    private BigDecimal maxWeightKg;
    private Long palletId;
    private String palletNo;
    private String palletType;
    private String palletStatus;
    private BigDecimal capacityPercent;
    private Integer skuKindCount;
}

