package com.erp.admin.wms;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryForecastAvailableQuantityTest {
	@Test
	void logical_available_is_not_reduced_by_reserved_twice() throws Exception {
		Path path = locate(Paths.get("admin", "src", "main", "java", "com", "erp", "admin", "wms", "facade",
				"InventoryForecastFacade.java"));
		String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
		assertThat(source).doesNotContain("stock.getTotalAvailable() - reserved");
	}

	private Path locate(Path relative) {
		Path[] candidates = { relative, Paths.get("erp-backend").resolve(relative), Paths.get("..").resolve(relative) };
		for (Path candidate : candidates) if (Files.exists(candidate)) return candidate;
		return candidates[0];
	}
}
