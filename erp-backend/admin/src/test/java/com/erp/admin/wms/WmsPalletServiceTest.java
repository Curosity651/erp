package com.erp.admin.wms;

import java.util.ArrayList;
import java.util.Arrays;

import com.erp.admin.wms.service.WmsPalletService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WmsPalletServiceTest {

    @Test
    void slot_codes_are_sorted_naturally() {
        ArrayList<String> slots = new ArrayList<>(Arrays.asList(
                "A10-01-L1-P01",
                "A2-01-L1-P01",
                "A1-01-L2-P01",
                "A1-01-L1-P02",
                "A1-01-L1-P01"));

        slots.sort(WmsPalletService::compareNatural);

        assertThat(slots).containsExactly(
                "A1-01-L1-P01",
                "A1-01-L1-P02",
                "A1-01-L2-P01",
                "A2-01-L1-P01",
                "A10-01-L1-P01");
    }
}
