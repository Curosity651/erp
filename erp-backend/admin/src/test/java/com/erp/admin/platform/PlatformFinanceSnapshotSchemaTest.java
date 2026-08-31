package com.erp.admin.platform;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformFinanceSnapshotSchemaTest {

    @Test
    void billing_records_are_bound_to_the_generated_monthly_bill_snapshot() throws Exception {
        String sql = new String(Files.readAllBytes(Paths.get("..", "sql", "migration",
                "V131__platform_finance_month_end_billing.sql")), StandardCharsets.UTF_8);
        String mapper = new String(Files.readAllBytes(Paths.get("src", "main", "resources", "mapper",
                "financial", "WmsMonthlyBillMapper.xml")), StandardCharsets.UTF_8);

        assertThat(sql).contains("monthly_bill_id", "rack_rent_total");
        assertThat(mapper).contains("r.monthly_bill_id = #{billId}");
    }
}
