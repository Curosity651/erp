package com.erp.admin.wms;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryEventQueryMapperSqlTest {
	@Test
	void stock_record_queries_use_the_new_event_ledger_only() throws Exception {
		String sql = new String(Files.readAllBytes(Paths.get(
				"src/main/resources/mapper/wms/InventoryEventQueryMapper.xml")), StandardCharsets.UTF_8)
				.toLowerCase();
		assertThat(sql).contains("wms_inventory_event", "wms_inventory_event_line", "erp_tenant_id");
		assertThat(sql).doesNotContain("wms_stock_flow", "wms_stock_posting", "wms_inventory ");
	}
}
