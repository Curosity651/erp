package com.erp.admin.platform.finance.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractRefundCheckVO {
    private Long contractId;
    private LocalDate eligibleDate;
    private boolean termSatisfied;
    private BigDecimal unpaidAmount;
    private boolean noDebt;
    private long remainingQuantity;
    private long reservedQuantity;
    private boolean noRemainingGoods;
    private boolean refundable;
    private String message;
}
