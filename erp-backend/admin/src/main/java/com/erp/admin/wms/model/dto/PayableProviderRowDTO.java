package com.erp.admin.wms.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 应付物流商——物流单级原始行（Java 侧按物流商汇总 + 下钻）。原币 USD。
 *
 * @author erp
 */
@Data
public class PayableProviderRowDTO {

    private Long shippingOrderId;
    private String shippingNo;
    private Long providerId;
    private String providerName;

    /** 物流总额 USD（原币） */
    private BigDecimal totalAmount;
    /** 物流总额 CNY（参考） */
    private BigDecimal totalAmountCny;

    /** 付款状态 0未付/1已付 */
    private Integer paymentStatus;

    private String shippingStatus;
}
