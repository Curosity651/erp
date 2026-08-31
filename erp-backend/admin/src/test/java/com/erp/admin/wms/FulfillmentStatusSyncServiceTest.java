package com.erp.admin.wms;

import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskLineMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import com.erp.admin.wms.service.FulfillmentStatusSyncService;
import com.erp.admin.wms.service.FulfillmentProgressService;
import com.erp.admin.wms.service.LocationInventoryService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class FulfillmentStatusSyncServiceTest {

	@Test
	void exception_cancel_without_picked_goods_releases_reservation_immediately() {
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		WmsFulfillmentPickTaskLineMapper lineMapper = mock(WmsFulfillmentPickTaskLineMapper.class);
		LocationInventoryService inventoryService = mock(LocationInventoryService.class);
		FulfillmentProgressService progressService = mock(FulfillmentProgressService.class);
		WmsFulfillmentOrder order = exceptionOrder();
		when(orderMapper.selectForUpdate(10L)).thenReturn(order);
		when(orderMapper.transit(10L, FulfillmentStatus.EXCEPTION,
				FulfillmentStatus.CANCELLED)).thenReturn(1);
		FulfillmentStatusSyncService service = new FulfillmentStatusSyncService(orderMapper,
				lineMapper, inventoryService, progressService);

		service.cancelFromException(10L, false, "货主取消");

		verify(inventoryService).release(eq(10L), argThat(context ->
				"fulfillment-release:10".equals(context.getIdempotencyKey())));
		verify(orderMapper).transit(10L, FulfillmentStatus.EXCEPTION,
				FulfillmentStatus.CANCELLED);
		verify(progressService).sync(order, FulfillmentStatus.CANCELLED);
	}

	@Test
	void exception_cancel_with_picked_goods_requires_return_before_release() {
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		WmsFulfillmentPickTaskLineMapper lineMapper = mock(WmsFulfillmentPickTaskLineMapper.class);
		LocationInventoryService inventoryService = mock(LocationInventoryService.class);
		FulfillmentProgressService progressService = mock(FulfillmentProgressService.class);
		WmsFulfillmentOrder order = exceptionOrder();
		when(orderMapper.selectForUpdate(10L)).thenReturn(order);
		when(orderMapper.transitWithReason(10L, FulfillmentStatus.EXCEPTION,
				FulfillmentStatus.CANCEL_RETURNING, "货主取消")).thenReturn(1);
		FulfillmentStatusSyncService service = new FulfillmentStatusSyncService(orderMapper,
				lineMapper, inventoryService, progressService);

		service.cancelFromException(10L, true, "货主取消");

		verify(inventoryService, never()).release(anyLong(), any());
		verify(orderMapper).transitWithReason(10L, FulfillmentStatus.EXCEPTION,
				FulfillmentStatus.CANCEL_RETURNING, "货主取消");
		verify(progressService).sync(order, FulfillmentStatus.CANCEL_RETURNING);
	}

	@Test
	void platform_cancel_after_picking_updates_status_and_reason_atomically() {
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		WmsFulfillmentPickTaskLineMapper lineMapper = mock(WmsFulfillmentPickTaskLineMapper.class);
		LocationInventoryService inventoryService = mock(LocationInventoryService.class);
		FulfillmentProgressService progressService = mock(FulfillmentProgressService.class);
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(10L);
		order.setErpTenantId(6L);
		order.setSourceType("OZON");
		order.setSourceOrderId(20L);
		order.setFulfillmentStatus(FulfillmentStatus.PICKING);
		when(orderMapper.selectBySource("OZON", 20L)).thenReturn(order);
		when(orderMapper.transitWithReason(10L, FulfillmentStatus.PICKING,
				FulfillmentStatus.CANCEL_RETURNING, "平台取消")).thenReturn(1);
		FulfillmentStatusSyncService service = new FulfillmentStatusSyncService(orderMapper, lineMapper,
				inventoryService, progressService);

		service.platformCancelled("OZON", 20L, "平台取消");

		verify(orderMapper).transitWithReason(10L, FulfillmentStatus.PICKING,
				FulfillmentStatus.CANCEL_RETURNING, "平台取消");
		verify(orderMapper, never()).updateById(any());
		verify(progressService).sync(order, FulfillmentStatus.CANCEL_RETURNING);
	}

	private WmsFulfillmentOrder exceptionOrder() {
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(10L);
		order.setErpTenantId(6L);
		order.setFulfillmentStatus(FulfillmentStatus.EXCEPTION);
		return order;
	}
}
