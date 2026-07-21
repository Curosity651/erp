package com.erp.admin.wms.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 应付物流商（按物流商汇总 + 下钻物流单）。原币 USD，附 CNY 参考。
 *
 * @author erp
 */
@Data
@Builder
public class PayableProviderVO {

    private Long providerId;
    private String providerName;

    /** 合同总额 USD */
    private BigDecimal contract;
    /** 合同总额 CNY（参考） */
    private BigDecimal contractCny;
    private BigDecimal paid;
    private BigDecimal outstanding;

    private List<OrderVO> orders;

    @Data
    @Builder
    public static class OrderVO {
        private String shippingNo;
        private String shippingStatus;
        private BigDecimal totalAmount;
        private BigDecimal totalAmountCny;
        private boolean paid;
        private BigDecimal outstanding;
    }
}
