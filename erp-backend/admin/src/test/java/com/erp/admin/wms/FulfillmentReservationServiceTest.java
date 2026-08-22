package com.erp.admin.wms;

import java.util.Collections;

import com.erp.admin.wms.model.dto.FulfillmentCreateCommand;
import com.erp.admin.wms.model.dto.InventoryReservationRequest;
import com.erp.admin.wms.model.entity.WmsFulfillmentItem;
import com.erp.admin.wms.service.FulfillmentReservationService;
import com.erp.admin.wms.service.LocationInventoryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FulfillmentReservationServiceTest {
	@Test
	void maps_order_owner_and_sku_to_location_reservation_request() {
		LocationInventoryService inventoryService = mock(LocationInventoryService.class);
		FulfillmentReservationService service = new FulfillmentReservationService(inventoryService);
		FulfillmentCreateCommand command = new FulfillmentCreateCommand();
		command.setTenantId(1L);
		command.setWmsTenantId(2L);
		command.setErpTenantId(3L);
		command.setWarehouseId(4L);
		FulfillmentCreateCommand.Item item = new FulfillmentCreateCommand.Item();
		item.setSkuCode("SKU-A");
		item.setQuality("GOOD");
		item.setQuantity(5);
		command.setItems(Collections.singletonList(item));

		WmsFulfillmentItem persistedItem = new WmsFulfillmentItem();
		persistedItem.setId(31L);
		persistedItem.setSkuCode("SKU-A");
		persistedItem.setQuality("GOOD");
		persistedItem.setQuantity(5);

		service.reserve(9L, command, Collections.singletonList(persistedItem));

		ArgumentCaptor<InventoryReservationRequest> captor = ArgumentCaptor.forClass(InventoryReservationRequest.class);
		verify(inventoryService).reserve(captor.capture());
		assertThat(captor.getValue().getFulfillmentOrderId()).isEqualTo(9L);
		assertThat(captor.getValue().getFulfillmentItemId()).isEqualTo(31L);
		assertThat(captor.getValue().getErpTenantId()).isEqualTo(3L);
		assertThat(captor.getValue().getQuantity()).isEqualTo(5);
	}
}
