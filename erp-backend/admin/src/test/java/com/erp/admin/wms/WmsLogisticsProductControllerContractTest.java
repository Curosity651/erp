package com.erp.admin.wms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WmsLogisticsProductControllerContractTest {

	@Test
	void create_and_update_use_separate_endpoints_and_permissions() throws IOException {
		Path source = locate(Paths.get("admin", "src", "main", "java", "com", "erp", "admin", "wms",
				"controller", "WmsLogisticsProductController.java"));
		String java = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

		assertThat(java).contains("public ApiResult<Void> create(");
		assertThat(java).contains("@PutMapping(\"/{id}\")");
		assertThat(java).contains("public ApiResult<Void> update(");
		assertThat(java).contains("wms:logistics-product:add", "wms:logistics-product:edit");
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
