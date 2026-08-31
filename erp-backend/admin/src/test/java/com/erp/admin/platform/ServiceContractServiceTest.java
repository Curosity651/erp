package com.erp.admin.platform;

import com.erp.admin.platform.finance.service.ServiceContractService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceContractServiceTest {

    @Test
    void warehouse_deposit_is_fixed_regardless_of_rack_count() {
        assertThat(ServiceContractService.calculateWarehouseDeposit()).isEqualByComparingTo("60000.00");
    }

    @Test
    void subscription_amount_follows_rack_count() {
        assertThat(ServiceContractService.calculateSubscriptionTotal(1)).isEqualByComparingTo("60000.00");
        assertThat(ServiceContractService.calculateSubscriptionTotal(3)).isEqualByComparingTo("180000.00");
    }

    @Test
    void rack_rent_is_collected_once_for_all_calendar_months_in_contract() {
        BigDecimal total = ServiceContractService.calculateRackRentTotal(
                2, new BigDecimal("20000"),
                LocalDate.of(2026, 8, 15), LocalDate.of(2026, 9, 2));

        assertThat(total).isEqualByComparingTo("80000.00");
    }
}
