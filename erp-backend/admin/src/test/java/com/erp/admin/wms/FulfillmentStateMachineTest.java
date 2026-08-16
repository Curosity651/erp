package com.erp.admin.wms;

import com.erp.admin.wms.model.enums.FulfillmentStatus;
import org.junit.jupiter.api.Test;

import static com.erp.admin.wms.model.enums.FulfillmentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class FulfillmentStateMachineTest {

	@Test
	void supports_normal_order_fulfillment_flow() {
		assertThat(DRAFT.canTransitTo(WAITING_SHELF)).isTrue();
		assertThat(WAITING_SHELF.canTransitTo(PLATFORM_PROCESSING)).isTrue();
		assertThat(PLATFORM_PROCESSING.canTransitTo(WAITING_PICK)).isTrue();
		assertThat(WAITING_PICK.canTransitTo(PICKING)).isTrue();
		assertThat(PICKING.canTransitTo(WAITING_PACK)).isTrue();
		assertThat(WAITING_PACK.canTransitTo(PACKED)).isTrue();
		assertThat(PACKED.canTransitTo(SHIPPED)).isTrue();
	}

	@Test
	void supports_cancellation_and_exception_recovery_only_at_valid_points() {
		assertThat(WAITING_SHELF.canTransitTo(CANCELLED)).isTrue();
		assertThat(WAITING_PICK.canTransitTo(CANCELLED)).isTrue();
		assertThat(PICKING.canTransitTo(CANCEL_RETURNING)).isTrue();
		assertThat(CANCEL_RETURNING.canTransitTo(CANCELLED)).isTrue();
		assertThat(EXCEPTION.canTransitTo(WAITING_SHELF)).isTrue();
		assertThat(SHIPPED.canTransitTo(CANCELLED)).isFalse();
		assertThat(CANCELLED.canTransitTo(WAITING_SHELF)).isFalse();
	}

	@Test
	void rejects_skipping_required_warehouse_steps() {
		for (FulfillmentStatus terminal : new FulfillmentStatus[] { SHIPPED, CANCELLED }) {
			assertThat(WAITING_SHELF.canTransitTo(terminal)).isEqualTo(terminal == CANCELLED);
		}
		assertThat(WAITING_SHELF.canTransitTo(PACKED)).isFalse();
		assertThat(WAITING_PICK.canTransitTo(SHIPPED)).isFalse();
	}
}
