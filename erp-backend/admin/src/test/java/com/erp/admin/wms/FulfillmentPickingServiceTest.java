package com.erp.admin.wms;

import java.util.Arrays;

import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import com.erp.admin.wms.model.vo.FulfillmentBatchResultVO;
import com.erp.admin.wms.service.FulfillmentPickingService;
import com.erp.admin.wms.service.FulfillmentPlatformActionService;
import com.erp.admin.wms.service.FulfillmentProgressService;
import com.erp.admin.wms.service.platform.PlatformActionResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class FulfillmentPickingServiceTest {
	@Test
	void batch_accept_keeps_successful_order_when_another_order_fails() {
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		FulfillmentPlatformActionService actions = mock(FulfillmentPlatformActionService.class);
		FulfillmentProgressService progress = mock(FulfillmentProgressService.class);
		FulfillmentPickingService service = new FulfillmentPickingService(orderMapper, actions, progress);
		when(orderMapper.selectById(1L)).thenReturn(order(1L));
		when(orderMapper.selectById(2L)).thenReturn(order(2L));
		when(actions.accept(1L)).thenReturn(PlatformActionResult.success("OK", null));
		when(actions.accept(2L)).thenThrow(new IllegalStateException("platform failed"));
		when(orderMapper.transit(1L, FulfillmentStatus.WAITING_SHELF, FulfillmentStatus.WAITING_PICK))
				.thenReturn(1);

		FulfillmentBatchResultVO result = service.accept(Arrays.asList(1L, 2L));

		assertThat(result.getSuccessIds()).containsExactly(1L);
		assertThat(result.getFailures()).containsEntry(2L, "platform failed");
		verify(progress).sync(org.mockito.ArgumentMatchers.any(WmsFulfillmentOrder.class),
				org.mockito.ArgumentMatchers.eq(FulfillmentStatus.WAITING_PICK));
	}

	private WmsFulfillmentOrder order(Long id) {
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(id);
		order.setFulfillmentStatus(FulfillmentStatus.WAITING_SHELF);
		return order;
	}
}
