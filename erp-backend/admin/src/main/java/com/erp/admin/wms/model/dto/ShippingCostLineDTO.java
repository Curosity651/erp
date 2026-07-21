package com.erp.admin.wms.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 物流成本原始行（物流单 × 明细 SKU）——Java 侧按件分摊到 SKU。
 * 仅含已发运物流单（status != PENDING）。
 *
 * @author erp
 */
@Data
public class ShippingCostLineDTO {

    private Long shippingOrderId;
    private String shippingNo;
    private Long providerId;

    /** 物流单总额 USD（原币） */
    private BigDecimal orderTotalUsd;
    /** 物流单总额 CNY（参考） */
    private BigDecimal orderTotalCny;

    private String skuCode;

    /** 该 SKU 在本物流单内的发货数量（分摊基数） */
    private Integer quantity;
}
