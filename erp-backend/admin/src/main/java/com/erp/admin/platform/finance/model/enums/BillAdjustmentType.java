package com.erp.admin.platform.finance.model.enums;

import java.math.BigDecimal;

public enum BillAdjustmentType {
    SUPPLEMENT,
    DEDUCTION;

    public BigDecimal apply(BigDecimal amount) {
        return this == DEDUCTION ? amount.abs().negate() : amount.abs();
    }
}
