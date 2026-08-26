package com.erp.admin.wms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetFinanceControllerContractTest {

	@Test
	void exposes_separate_asset_and_payable_overviews_while_keeping_legacy_overview() throws IOException {
		String source = read(Paths.get("admin", "src", "main", "java", "com", "erp", "admin", "wms",
				"controller", "AssetFinanceController.java"));

		assertThat(source).contains("@GetMapping(\"/assets-overview\")");
		assertThat(source).contains("@GetMapping(\"/payables-overview\")");
		assertThat(source).contains("@GetMapping(\"/overview\")");
	}

	@Test
	void asset_overview_exposes_fbo_snapshot_freshness() throws IOException {
		String source = read(Paths.get("admin", "src", "main", "java", "com", "erp", "admin", "wms", "model",
				"vo", "AssetOverviewVO.java"));

		assertThat(source).contains("LocalDateTime fboLastSyncedAt");
		assertThat(source).contains("Boolean fboStale");
	}

	private String read(Path relative) throws IOException {
		Path[] candidates = { relative, Paths.get("erp-backend").resolve(relative), Paths.get("..").resolve(relative) };
		for (Path candidate : candidates) {
			if (Files.exists(candidate)) {
				return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
			}
		}
		return new String(Files.readAllBytes(candidates[0]), StandardCharsets.UTF_8);
	}
}
