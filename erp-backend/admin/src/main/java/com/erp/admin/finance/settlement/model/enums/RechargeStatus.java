package com.erp.admin.finance.settlement.model.enums;

public enum RechargeStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    REVERSED;

    public boolean canApprove() {
        return this == PENDING;
    }

    public boolean canCancel() {
        return this == PENDING;
    }

    public boolean canReverse() {
        return this == APPROVED;
    }
}
