package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class OutboundHandlingPreviewVO {
    private List<PalletCandidate> wholePallets = new ArrayList<>();
    private Integer wholePalletQuantity;
    private Integer looseQuantity;
    private BigDecimal estimatedFee;

    @Data
    public static class PalletCandidate {
        private Long palletId;
        private String palletNo;
        private Integer quantity;
        private Integer skuKinds;
    }
}

