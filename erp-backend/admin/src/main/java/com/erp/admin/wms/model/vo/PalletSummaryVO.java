package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PalletSummaryVO {
    private Long id;
    private String palletNo;
    private Long warehouseId;
    private String warehouseName;
    private String slotCode;
    private String palletType;
    private String palletStatus;
    private BigDecimal capacityPercent;
    private String capacitySource;
    private BigDecimal estimatedWeightKg;
    private BigDecimal actualWeightKg;
    private Integer skuKindCount;
    private Integer wholePalletEligible;
    private LocalDateTime createTime;
    private List<PalletItemVO> items;

    @Data
    public static class PalletItemVO {
        private Long erpTenantId;
        private String ownerName;
        private String skuCode;
        private Integer quantity;
        private Integer reservedQty;
        private String quality;
        private String inboundDate;
    }
}

