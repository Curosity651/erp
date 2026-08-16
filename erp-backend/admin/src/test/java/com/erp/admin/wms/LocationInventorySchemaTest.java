package com.erp.admin.wms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocationInventorySchemaTest {

	@Test
	void migration_defines_owner_sku_quality_uniqueness_and_reservation_guards() throws IOException {
		Path migration = locateMigration();

		assertThat(migration).exists();
		String sql = new String(Files.readAllBytes(migration), StandardCharsets.UTF_8);
		assertThat(sql).contains("uk_location_inventory_owner_sku_quality");
		assertThat(sql).contains("reserved_quantity");
		assertThat(sql).contains("version");
		assertThat(sql).contains("CHECK (quantity >= 0 AND reserved_quantity >= 0");
	}

	private Path locateMigration() {
		Path[] candidates = {
				Paths.get("sql", "migration", "V103__logical_location_inventory_core.sql"),
				Paths.get("..", "sql", "migration", "V103__logical_location_inventory_core.sql"),
				Paths.get("..", "..", "sql", "migration", "V103__logical_location_inventory_core.sql")
		};
		for (Path candidate : candidates) {
			if (Files.exists(candidate)) {
				return candidate;
			}
		}
		return candidates[0];
	}

}
