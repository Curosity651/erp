package com.erp.admin.finance.settlement;

import com.erp.admin.finance.settlement.model.enums.RechargeStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RechargeStatusPolicyTest {
    @Test
    void pendingCanBeReviewedAndCancelledButApprovedCanOnlyBeReversed() {
        assertTrue(RechargeStatus.PENDING.canApprove());
        assertTrue(RechargeStatus.PENDING.canCancel());
        assertFalse(RechargeStatus.APPROVED.canApprove());
        assertFalse(RechargeStatus.APPROVED.canCancel());
        assertTrue(RechargeStatus.APPROVED.canReverse());
    }
}
