package com.erp.admin.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.service.FulfillmentRecipientSnapshotService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FulfillmentRecipientSnapshotServiceTest {

	@Test
	void snapshots_recipient_fields_when_platform_payload_contains_them() {
		ErpOrder order = new ErpOrder();
		order.setRawJson("{\"customer\":{\"customer_name\":\"Ivan Petrov\","
				+ "\"phone\":\"+79990001122\",\"delivery_address\":\"Moscow, Tverskaya 1\"}}");

		FulfillmentRecipientSnapshotService.Snapshot snapshot =
				new FulfillmentRecipientSnapshotService(new ObjectMapper()).from(order);

		assertThat(snapshot.getName()).isEqualTo("Ivan Petrov");
		assertThat(snapshot.getPhone()).isEqualTo("+79990001122");
		assertThat(snapshot.getAddress()).isEqualTo("Moscow, Tverskaya 1");
	}

	@Test
	void missing_or_malformed_platform_payload_leaves_snapshot_empty() {
		ErpOrder order = new ErpOrder();
		order.setRawJson("not-json");

		FulfillmentRecipientSnapshotService.Snapshot snapshot =
				new FulfillmentRecipientSnapshotService(new ObjectMapper()).from(order);

		assertThat(snapshot.getName()).isNull();
		assertThat(snapshot.getPhone()).isNull();
		assertThat(snapshot.getAddress()).isNull();
	}
}
