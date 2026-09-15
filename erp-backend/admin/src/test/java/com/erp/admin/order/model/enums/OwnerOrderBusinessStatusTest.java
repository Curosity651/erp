package com.erp.admin.order.model.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.erp.admin.order.model.entity.ErpOrder;
import org.junit.jupiter.api.Test;

class OwnerOrderBusinessStatusTest {

	@Test
	void shouldMapNormalWarehouseLifecycle() {
		assertStatus(null, OwnerOrderBusinessStatus.PENDING_CONFIRM);
		assertStatus("WAITING_SHELF", OwnerOrderBusinessStatus.WAITING_SHELF);
		assertStatus("WAITING_PICK", OwnerOrderBusinessStatus.OUTBOUND_PROCESSING);
		assertStatus("PICKING", OwnerOrderBusinessStatus.OUTBOUND_PROCESSING);
		assertStatus("WAITING_PACK", OwnerOrderBusinessStatus.OUTBOUND_PROCESSING);
		assertStatus("PACKED", OwnerOrderBusinessStatus.OUTBOUND_PROCESSING);
		assertStatus("SHIPPED", OwnerOrderBusinessStatus.HANDED_OVER);
	}

	@Test
	void shouldPreserveCancellationAndException() {
		assertStatus("CANCELLED", OwnerOrderBusinessStatus.CANCELLED);
		assertStatus("EXCEPTION", OwnerOrderBusinessStatus.EXCEPTION);
		ErpOrder cancelledBeforeSubmission = new ErpOrder();
		cancelledBeforeSubmission.setErpStatus("CANCELED");
		assertEquals(OwnerOrderBusinessStatus.CANCELLED,
				OwnerOrderBusinessStatus.resolve(cancelledBeforeSubmission));
	}

	private void assertStatus(String warehouseStatus, OwnerOrderBusinessStatus expected) {
		ErpOrder order = new ErpOrder();
		order.setWarehouseFulfillmentStatus(warehouseStatus);
		assertEquals(expected, OwnerOrderBusinessStatus.resolve(order));
	}
}
