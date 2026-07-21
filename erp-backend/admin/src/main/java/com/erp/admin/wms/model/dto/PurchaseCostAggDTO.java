package com.erp.admin.wms.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购成本聚合（按 SKU × 币种）——加权平均采购单价的取数底座。
 *
 * @author erp
 */
@Data
public class PurchaseCostAggDTO {

    private String skuCode;

    /** 采购币种 */
    private String currency;

    /** 累计采购数量 */
    private Integer qty;

    /** 累计采购金额 = Σ(数量 × 单价) */
    private BigDecimal amount;
}
