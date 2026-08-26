package com.erp.admin.finance.settlement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogisticsProductCurrencyRepairMigrationTest {

    @Test
    void migration_repairs_product_order_snapshot_and_billing_currency() throws IOException {
        Path source = locate(Paths.get("sql", "migration", "V135__repair_logistics_product_currency_to_cny.sql"));
        String sql = new String(Files.readAllBytes(source), StandardCharsets.UTF_8).toLowerCase();

        assertThat(sql).contains("update wms_logistics_product");
        assertThat(sql).contains("update wms_fulfillment_order");
        assertThat(sql).contains("update wms_client_billing_record");
        assertThat(sql).contains("currency = 'cny'");
        assertThat(sql).contains("fulfillment_logistics:");
    }

    private Path locate(Path relative) {
        Path[] candidates = { relative, Paths.get("erp-backend").resolve(relative),
                Paths.get("..").resolve(relative) };
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) return candidate;
        }
        return candidates[0];
    }
}
