package com.erp.admin.wms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorFinanceMapperSqlTest {

	@Test
	void income_summary_groups_by_currency_and_detail_supports_fulfillment_orders() throws IOException {
		Path mapper = locate(Paths.get("admin", "src", "main", "resources", "mapper", "wms",
				"OperatorFinanceMapper.xml"));
		String xml = new String(Files.readAllBytes(mapper), StandardCharsets.UTF_8);

		assertThat(xml).contains("r.currency AS currency");
		assertThat(xml).contains("GROUP BY r.logistics_product_id, r.bill_month, r.currency");
		assertThat(xml).contains("LEFT JOIN wms_fulfillment_order f ON f.id = r.fulfillment_order_id");
		assertThat(xml).contains("COALESCE(f.fulfillment_no, o.outbound_no, f.source_order_no, r.biz_id) AS businessNo");
		assertThat(xml).contains("f.source_order_no AS platformOrderId");
		assertThat(xml).contains("r.product_name_snapshot AS productNameSnapshot");
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
