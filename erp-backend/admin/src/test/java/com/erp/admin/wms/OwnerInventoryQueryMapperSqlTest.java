package com.erp.admin.wms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerInventoryQueryMapperSqlTest {

	@Test
	void mapper_uses_logical_location_inventory_and_calculates_available_once() throws IOException {
		Path mapper = locate(Paths.get("admin", "src", "main", "resources", "mapper", "wms",
				"OwnerInventoryQueryMapper.xml"));
		assertThat(mapper).exists();
		String sql = new String(Files.readAllBytes(mapper), StandardCharsets.UTF_8);
		assertThat(sql).contains("wms_location_inventory", "li.quantity - li.reserved_quantity",
				"li.erp_tenant_id", "li.deleted = 0", "li.quality = 'DEFECTIVE'");
		assertThat(sql).doesNotContain("FROM wms_inventory", "wms_stock_flow", "wms_physical_inventory");
	}

	private Path locate(Path relative) {
		Path[] candidates = { relative, Paths.get("erp-backend").resolve(relative), Paths.get("..").resolve(relative) };
		for (Path candidate : candidates) if (Files.exists(candidate)) return candidate;
		return candidates[0];
	}
}
