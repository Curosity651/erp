package com.erp.admin.wms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryEventSchemaTest {

	@Test
	void migration_creates_event_ledger_and_normalizes_quality() throws IOException {
		Path file = locate(Paths.get("sql", "migration", "V137__inventory_event_ledger_and_quality.sql"));
		String sql = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		assertThat(sql).contains("CREATE TABLE IF NOT EXISTS wms_inventory_event",
				"CREATE TABLE IF NOT EXISTS wms_inventory_event_line",
				"uk_inventory_event_idempotency", "uk_inventory_event_line_no",
				"SET quality = 'DEFECTIVE'", "chk_location_inventory_quality");
	}

	private Path locate(Path relative) {
		Path[] candidates = { relative, Paths.get("erp-backend").resolve(relative), Paths.get("..").resolve(relative) };
		for (Path candidate : candidates) if (Files.exists(candidate)) return candidate;
		return candidates[0];
	}
}
