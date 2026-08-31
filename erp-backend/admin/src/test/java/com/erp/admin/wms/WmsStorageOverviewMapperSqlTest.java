package com.erp.admin.wms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WmsStorageOverviewMapperSqlTest {

	@Test
	void capacity_scope_and_inventory_ownership_are_independent() throws IOException {
		Path mapper = locate(Paths.get("admin", "src", "main", "resources", "mapper", "wms",
				"WmsStorageOverviewMapper.xml"));
		String xml = new String(Files.readAllBytes(mapper), StandardCharsets.UTF_8);

		assertThat(xml).contains("owned_inventory AS");
		assertThat(xml).contains("FROM wms_location_inventory li");
		assertThat(xml).contains("li.wms_tenant_id = #{wmsTenantId}");
		assertThat(xml).contains("li.location_id = a.location_id");
		assertThat(xml).contains("li.deleted = 0");
		assertThat(xml).contains("t.parent_wms_tenant_id = #{wmsTenantId}");
		assertThat(xml).contains("COALESCE(l.public_shared, 0) = 0");
		assertThat(xml).contains("SELECT warehouse_id FROM owned_inventory");
		assertThat(xml).doesNotContain("wms_physical_inventory");
	}

	private Path locate(Path relative) {
		Path[] candidates = { relative, Paths.get("erp-backend").resolve(relative), Paths.get("..").resolve(relative) };
		for (Path candidate : candidates) {
			if (Files.exists(candidate)) {
				return candidate;
			}
		}
		return candidates[0];
	}
}
