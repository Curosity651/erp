package com.erp.admin.wms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WmsLogisticsProductMapperSqlTest {

	@Test
	void legacy_outbound_reference_query_matches_table_without_deleted_column() throws IOException {
		Path mapper = locate(Paths.get("admin", "src", "main", "java", "com", "erp", "admin", "wms",
				"mapper", "WmsLogisticsProductMapper.java"));
		String source = new String(Files.readAllBytes(mapper), StandardCharsets.UTF_8);

		assertThat(source).contains("countLegacyOutboundReferences");
		assertThat(source).doesNotContain("o.deleted = 0");
	}

	private Path locate(Path relative) {
		Path[] candidates = {
				relative,
				Paths.get("erp-backend").resolve(relative),
				Paths.get("..").resolve(relative),
				Paths.get("..", "..").resolve(relative)
		};
		for (Path candidate : candidates) {
			if (Files.exists(candidate)) return candidate;
		}
		return candidates[0];
	}
}
