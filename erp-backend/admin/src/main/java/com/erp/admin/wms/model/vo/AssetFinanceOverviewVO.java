package com.erp.admin.wms.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资产与账务总览（分币种，不折算）。
 *
 * @author erp
 */
@Data
@Builder
public class AssetFinanceOverviewVO {

    /** 资产按币种：采购成本 + 物流附加（物流仅 USD 行有值） */
    private List<AssetCurrencyVO> assets;

    /** 应付供应商按币种 */
    private List<PayableCurrencyVO> supplierPayable;

    /** 应付物流商（USD 单一币种） */
    private PayableCurrencyVO providerPayable;

    /** Quantity that is owned but has no traceable purchase cost. */
    private Integer unvaluedSkuCount;
    private Integer unvaluedQuantity;
    private Integer totalHeldQuantity;
    private List<HoldingPositionVO> holdingPositions;

    @Data
    @Builder
    public static class HoldingPositionVO {
        private String code;
        private String name;
        private Integer quantity;
    }

    @Data
    @Builder
    public static class AssetCurrencyVO {
        private String currency;
        private BigDecimal procurement;
        private BigDecimal logistics;
        private BigDecimal total;
    }

    @Data
    @Builder
    public static class PayableCurrencyVO {
        private String currency;
        private BigDecimal contract;
        private BigDecimal paid;
        private BigDecimal outstanding;
    }
}
