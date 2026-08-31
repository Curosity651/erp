package com.erp.admin.order;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboundFlowIsolationSqlTest {

	@Test
	void legacy_outbound_queries_exclude_orders_already_in_new_fulfillment() throws IOException {
		String xml;
		try (InputStream input = getClass().getResourceAsStream("/mapper/order/ErpOrderMapper.xml")) {
			assertThat(input).isNotNull();
			try (Scanner scanner = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
				xml = scanner.hasNext() ? scanner.next() : "";
			}
		}

		assertThat(xml).contains("AND fulfillment_order_id IS NULL");
		assertThat(xml).contains("AND outbound_order_id IS NULL");
		assertThat(xml).contains("AND o.fulfillment_order_id IS NULL");
		assertThat(xml).contains("AND o.outbound_order_id IS NULL");
	}
}
