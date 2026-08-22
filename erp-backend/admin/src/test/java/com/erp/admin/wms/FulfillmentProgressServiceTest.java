package com.erp.admin.wms;

import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import com.erp.admin.wms.service.FulfillmentProgressService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class FulfillmentProgressServiceTest {

	@Test
	void platform_fulfillment_progress_is_written_by_owner_scope() {
		ErpOrderMapper mapper = mock(ErpOrderMapper.class);
		when(mapper.updateWarehouseFulfillmentStatus(20L, 6L, "PICKING", 10L)).thenReturn(1);
		FulfillmentProgressService service = new FulfillmentProgressService(mapper);
		WmsFulfillmentOrder order = order("OZON");

		service.sync(order, FulfillmentStatus.PICKING);

		verify(mapper).updateWarehouseFulfillmentStatus(20L, 6L, "PICKING", 10L);
	}

	@Test
	void manual_fulfillment_has_no_erp_order_to_update() {
		ErpOrderMapper mapper = mock(ErpOrderMapper.class);
		FulfillmentProgressService service = new FulfillmentProgressService(mapper);

		service.sync(order("MANUAL"), FulfillmentStatus.PACKED);

		verifyNoInteractions(mapper);
	}

	private WmsFulfillmentOrder order(String sourceType) {
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(10L);
		order.setErpTenantId(6L);
		order.setSourceType(sourceType);
		order.setSourceOrderId(20L);
		return order;
	}
}
