package com.erp.admin.order;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyOutboundRetirementMigrationTest {

	@Test
	void migration_hides_legacy_menu_repairs_shop_defaults_and_orphan_links() throws IOException {
		Path migration = locate(Paths.get("sql", "migration", "V128__retire_legacy_outbound_entry.sql"));
		String sql = new String(Files.readAllBytes(migration), StandardCharsets.UTF_8);

		assertThat(sql).contains("id = 160600", "hidden = 1", "DELETE FROM sys_role_menu");
		assertThat(sql).contains("default_wms_warehouse_id", "warehouse_type = 'OWN'");
		assertThat(sql).contains("outbound_order_id = NULL", "NOT EXISTS");
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
