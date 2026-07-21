package com.erp.admin.wms.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 应付供应商（按供应商 × 币种汇总 + 下钻采购单）。
 *
 * @author erp
 */
@Data
@Builder
public class PayableSupplierVO {

    private Long supplierId;
    private String supplierName;
    private String currency;

    /** 合同总额 */
    private BigDecimal contract;
    /** 已付 = 已付预付款 + 已付尾款 */
    private BigDecimal paid;
    /** 应付余额 = 合同 − 已付 */
    private BigDecimal outstanding;

    private List<OrderVO> orders;

    @Data
    @Builder
    public static class OrderVO {
        private String orderNo;
        private String orderStatus;
        private BigDecimal totalAmount;
        private BigDecimal prepayAmount;
        /** 尾款 = 合同 − 首付款（采购单无独立尾款金额字段，此处推算） */
        private BigDecimal balanceAmount;
        private boolean prepayPaid;
        private boolean balancePaid;
        private BigDecimal paid;
        private BigDecimal outstanding;
        private LocalDateTime prepayTime;
        private LocalDateTime balancePayTime;
    }
}
