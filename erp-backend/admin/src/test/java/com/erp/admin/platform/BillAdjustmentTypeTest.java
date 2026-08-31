package com.erp.admin.platform;

import com.erp.admin.platform.finance.model.enums.BillAdjustmentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BillAdjustmentTypeTest {

    @Test
    void supplement_is_positive_and_deduction_is_negative() {
        assertThat(BillAdjustmentType.SUPPLEMENT.apply(new BigDecimal("12.50")))
                .isEqualByComparingTo("12.50");
        assertThat(BillAdjustmentType.DEDUCTION.apply(new BigDecimal("12.50")))
                .isEqualByComparingTo("-12.50");
    }
}
