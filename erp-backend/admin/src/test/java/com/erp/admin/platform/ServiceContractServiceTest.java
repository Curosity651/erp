package com.erp.admin.platform;

import com.erp.admin.platform.finance.service.ServiceContractService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
}
