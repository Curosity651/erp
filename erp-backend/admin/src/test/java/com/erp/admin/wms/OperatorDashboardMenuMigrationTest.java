package com.erp.admin.wms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorDashboardMenuMigrationTest {

	@Test
	void duplicate_provider_dashboard_is_removed_without_losing_read_permission() throws IOException {
		Path migration = locate(Paths.get("sql", "migration", "V126__remove_duplicate_provider_dashboard.sql"));
		assertThat(migration).exists();

		String sql = new String(Files.readAllBytes(migration), StandardCharsets.UTF_8);
		assertThat(sql).contains("parent_id = 900400", "id = 180501");
		assertThat(sql).contains("menu_id = 180500");
		assertThat(sql).contains("id = 180500", "deleted = 180500");
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
