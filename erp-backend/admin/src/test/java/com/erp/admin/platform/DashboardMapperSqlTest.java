package com.erp.admin.platform;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards dashboard statistics against accidentally returning to retired outbound and inventory tables.
 */
class DashboardMapperSqlTest {

    @Test
    void platform_dashboard_uses_fulfillment_orders_for_outbound_metrics() throws IOException {
        String xml = readMapper("statistics", "PlatformDashboardMapper.xml");

        assertThat(xml).contains("wms_fulfillment_order", "wms_fulfillment_item");
        assertThat(xml).doesNotContain("wms_sales_outbound_order");
    }

    @Test
    void platform_dashboard_uses_logical_location_inventory_for_stock_metrics() throws IOException {
        String xml = readMapper("statistics", "PlatformDashboardMapper.xml");

        assertThat(xml).contains("wms_location_inventory");
        assertThat(xml).doesNotContain("wms_physical_inventory");
    }

    @Test
    void operator_dashboard_uses_current_fulfillment_and_logical_inventory_tables() throws IOException {
        String xml = readMapper("wms", "OperatorDashboardMapper.xml");

        assertThat(xml).contains("wms_fulfillment_order", "wms_location_inventory");
        assertThat(xml).doesNotContain("wms_sales_outbound_order", "wms_physical_inventory",
                "FROM wms_inventory");
    }

    @Test
    void erp_sku_revenue_is_allocated_from_actual_order_revenue() throws IOException {
        String xml = readMapper("statistics", "DashboardMapper.xml");

        assertThat(xml).contains("o.total_amount_rub * oi.item_amount_rub / item_totals.amount_sum");
        assertThat(xml).doesNotContain("COALESCE(SUM(oi.item_amount_rub), 0) AS totalAmount");
    }

    @Test
    void sales_target_actuals_match_the_dashboard_sales_definition() throws IOException {
        String xml = readMapper("order", "ErpOrderMapper.xml");

        assertThat(xml).contains("YEAR(platform_created_at_moscow)",
                "MONTH(platform_created_at_moscow)", "erp_status != 'CANCELED'");
    }

    @Test
    void migration_activates_the_real_platform_dashboard_page() throws IOException {
        Path migration = locate(Paths.get("sql", "migration", "V122__repair_dashboard_data_sources.sql"));
        String sql = new String(Files.readAllBytes(migration), StandardCharsets.UTF_8);

        assertThat(sql).contains("dashboard/platform/index", "900300");
    }

    private String readMapper(String group, String name) throws IOException {
        Path mapper = locate(Paths.get("admin", "src", "main", "resources", "mapper", group, name));
        return new String(Files.readAllBytes(mapper), StandardCharsets.UTF_8);
    }

    private Path locate(Path relative) {
        Path[] candidates = {
                relative,
                Paths.get("erp-backend").resolve(relative),
                Paths.get("..").resolve(relative),
                Paths.get("..", "..").resolve(relative)
        };
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return candidates[0];
    }
}
