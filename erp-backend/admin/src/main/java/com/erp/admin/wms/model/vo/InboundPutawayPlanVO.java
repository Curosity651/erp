package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class InboundPutawayPlanVO {
    private Long inboundOrderId;
    private Long warehouseId;
    private List<PalletPlan> pallets = new ArrayList<>();
    private List<PalletSlotVO> slotCandidates = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private BigDecimal calculatedVolumeCbm;
    private List<String> volumeMissingSkuCodes = new ArrayList<>();

    @Data
    public static class PalletPlan {
        private String palletKey;
        private Long palletId;
        private String palletNo;
        private Boolean existingPallet;
        private String palletType;
        private String quality;
        private String slotCode;
        private String locationCode;
        private Integer levelNo;
        private BigDecimal capacityPercent;
        private String capacitySource;
        private BigDecimal estimatedWeightKg;
        private Boolean wholePalletEligible;
        private List<PalletPlanItem> items = new ArrayList<>();
    }

    @Data
    public static class PalletPlanItem {
        private Long erpTenantId;
        private String skuCode;
        private String skuName;
        private Integer quantity;
        private Integer quantityPerPallet;
    }
}
