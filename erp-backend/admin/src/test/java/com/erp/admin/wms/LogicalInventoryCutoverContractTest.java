package com.erp.admin.wms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogicalInventoryCutoverContractTest {

	@Test
	void active_warehouse_and_finance_services_only_use_logical_location_inventory() throws IOException {
		assertNoLegacyInventory("admin/src/main/java/com/erp/admin/wms/service/WmsRackAssignmentService.java");
		assertNoLegacyInventory("admin/src/main/java/com/erp/admin/wms/service/WmsZoneService.java");
		assertNoLegacyInventory("admin/src/main/java/com/erp/admin/wms/service/WmsStructureLockService.java");
		assertNoLegacyInventory("admin/src/main/java/com/erp/admin/platform/finance/service/ServiceContractService.java");
		assertNoLegacyInventory("admin/src/main/java/com/erp/admin/wms/controller/WmsLocationManageController.java");
	}

	@Test
	void logical_inventory_mapper_provides_all_active_occupancy_aggregates() throws IOException {
		String java = read("admin/src/main/java/com/erp/admin/wms/mapper/WmsLocationInventoryMapper.java");
		String xml = read("admin/src/main/resources/mapper/wms/WmsLocationInventoryMapper.xml");

		assertThat(java).contains("listOccupiedLocationCodes", "listBlockingWmsTenantIdsByRack",
				"sumQuantityByProviderAndWarehouse", "sumReservedByProviderAndWarehouse");
		assertThat(xml).contains("i.quantity &gt; 0 OR i.reserved_quantity &gt; 0", "JOIN wms_location l",
				"l.rack_no = #{rackNo}");
	}

	@Test
	void operator_dashboard_reads_the_logical_inventory_core() throws IOException {
		String xml = read("admin/src/main/resources/mapper/wms/OperatorDashboardMapper.xml");
		assertThat(xml).contains("FROM wms_location_inventory i");
		assertThat(xml).doesNotContain("FROM wms_physical_inventory i");
	}

	@Test
	void migration_aligns_new_core_collations_with_existing_sku_tables() throws IOException {
		String sql = read("sql/migration/V138__align_logical_inventory_collations.sql");
		assertThat(sql).contains("ALTER TABLE wms_location_inventory", "ALTER TABLE wms_inventory_reservation",
				"ALTER TABLE wms_inventory_event", "ALTER TABLE wms_inventory_event_line",
				"COLLATE utf8mb4_0900_ai_ci");
	}

	@Test
	void retired_outbound_controllers_guard_every_legacy_write_endpoint() throws IOException {
		assertEveryLegacyWriteIsGuarded("admin/src/main/java/com/erp/admin/wms/controller/OutboundPickingController.java");
		assertEveryLegacyWriteIsGuarded("admin/src/main/java/com/erp/admin/wms/controller/OutboundShippingController.java");
	}

	private void assertEveryLegacyWriteIsGuarded(String relative) throws IOException {
		String source = read(relative);
		assertThat(source).contains("private final WmsCoreModeGuard coreModeGuard;");
		assertThat(count(source, "@PostMapping")).isEqualTo(count(source, "coreModeGuard.assertLegacyWriteAllowed"));
	}

	private int count(String source, String token) {
		int count = 0;
		int offset = 0;
		while ((offset = source.indexOf(token, offset)) >= 0) {
			count++;
			offset += token.length();
		}
		return count;
	}

	private void assertNoLegacyInventory(String relative) throws IOException {
		assertThat(read(relative)).doesNotContain("WmsPhysicalInventoryMapper", "WmsPhysicalInventoryService",
				"wms_physical_inventory");
	}

	private String read(String relative) throws IOException {
		Path path = locate(Paths.get(relative));
		assertThat(path).exists();
		return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
	}

	private Path locate(Path relative) {
		Path[] candidates = { relative, Paths.get("erp-backend").resolve(relative),
				Paths.get("..").resolve(relative), Paths.get("..", "..").resolve(relative) };
		for (Path candidate : candidates) {
			if (Files.exists(candidate)) return candidate;
		}
		return candidates[0];
	}
}
