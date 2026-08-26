package com.erp.admin.wms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetFinanceMapperSqlTest {

	@Test
	void asset_queries_use_logical_location_inventory_and_explicit_owner_scope() throws IOException {
		Path mapper = locate(Paths.get("admin", "src", "main", "resources", "mapper", "wms",
				"AssetFinanceMapper.xml"));
		String xml = new String(Files.readAllBytes(mapper), StandardCharsets.UTF_8);

		assertThat(xml).contains("FROM wms_location_inventory li");
		assertThat(xml).contains("li.erp_tenant_id = #{erpTenantId}");
		assertThat(xml).contains("po.erp_tenant_id = #{erpTenantId}");
		assertThat(xml).contains("so.erp_tenant_id = #{erpTenantId}");
		assertThat(xml).doesNotContain("FROM wms_inventory i");
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
