package com.erp.admin.wms;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FulfillmentShippingMapperSqlTest {

	@Test
	void shipping_page_keeps_shipped_orders_and_supports_required_filters() throws IOException {
		String xml;
		try (InputStream input = getClass().getResourceAsStream("/mapper/wms/WmsFulfillmentOrderMapper.xml")) {
			assertThat(input).isNotNull();
			try (Scanner scanner = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
				xml = scanner.hasNext() ? scanner.next() : "";
			}
		}

		assertThat(xml).contains("selectShippingPage");
		assertThat(xml).contains("'WAITING_PACK', 'PACKED', 'SHIPPED'");
		assertThat(xml).contains("query.erpTenantId");
		assertThat(xml).contains("query.warehouseId");
		assertThat(xml).contains("query.fulfillmentStatus");
		assertThat(xml).contains("query.shippedBy");
		assertThat(xml).contains("query.startDate");
		assertThat(xml).contains("query.endDate");
	}
}
