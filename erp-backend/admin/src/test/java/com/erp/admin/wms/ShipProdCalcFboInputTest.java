package com.erp.admin.wms;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShipProdCalcFboInputTest {
	@Test
	void calculation_uses_fbo_snapshot_instead_of_zero() throws Exception {
		Path file = locate(Paths.get("admin", "src", "main", "java", "com", "erp", "admin", "wms", "facade",
				"ShipProdCalcFacade.java"));
		String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		assertThat(source).contains("FboInventorySnapshotService", "fboBySku");
		assertThat(source).doesNotContain(".fbo(0)");
	}

	private Path locate(Path relative) {
		Path[] candidates = { relative, Paths.get("erp-backend").resolve(relative), Paths.get("..").resolve(relative) };
		for (Path candidate : candidates) if (Files.exists(candidate)) return candidate;
		return candidates[0];
	}
}
