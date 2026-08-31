package com.erp.admin.platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MonthlyBillCurrencySchemaTest {

	@Test
	void monthly_bill_schema_records_currency_and_supports_provider_queries() throws IOException {
		Path migration = locate(Paths.get("sql", "migration", "V127__provider_finance_currency_and_indexes.sql"));
		assertThat(migration).exists();

		String sql = new String(Files.readAllBytes(migration), StandardCharsets.UTF_8);
		assertThat(sql).contains("ADD COLUMN currency", "DEFAULT 'CNY'");
		assertThat(sql).contains("idx_monthly_bill_tenant_month_status_currency");
		assertThat(sql).contains("idx_client_billing_tenant_month_currency");
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
