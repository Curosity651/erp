package com.erp.admin.wms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorDashboardMapperSqlTest {

	@Test
	void dashboard_queries_keep_currency_and_expense_status_dimensions() throws IOException {
		Path mapper = locate(Paths.get("admin", "src", "main", "resources", "mapper", "wms",
				"OperatorDashboardMapper.xml"));
		String xml = new String(Files.readAllBytes(mapper), StandardCharsets.UTF_8);

		assertThat(xml).contains("r.currency AS currency");
		assertThat(xml).contains("GROUP BY r.bill_month, r.currency");
		assertThat(xml).contains("b.currency AS currency", "b.status AS status");
		assertThat(xml).contains("GROUP BY b.bill_month, b.currency, b.status");
		assertThat(xml).contains("GROUP BY r.logistics_product_id, r.currency");
		assertThat(xml).contains("GROUP BY r.erp_tenant_id, t.tenant_name, r.currency");
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
