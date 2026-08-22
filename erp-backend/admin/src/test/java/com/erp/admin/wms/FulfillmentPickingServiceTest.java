package com.erp.admin.wms;

import java.util.Arrays;
import java.util.Collections;

import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentItemMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskLineMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskOrderMapper;
import com.erp.admin.wms.mapper.WmsInventoryReservationMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.dto.FulfillmentPickScanDTO;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskLine;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskOrder;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

class FulfillmentPickingServiceTest {
	@Test
	void picked_order_waits_for_pack_before_next_order_is_unlocked() {
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		WmsFulfillmentPickTaskMapper taskMapper = mock(WmsFulfillmentPickTaskMapper.class);
		WmsFulfillmentPickTaskOrderMapper taskOrderMapper = mock(WmsFulfillmentPickTaskOrderMapper.class);
		WmsFulfillmentPickTaskLineMapper lineMapper = mock(WmsFulfillmentPickTaskLineMapper.class);
		FulfillmentProgressService progress = mock(FulfillmentProgressService.class);
		FulfillmentPickingService service = new FulfillmentPickingService(orderMapper,
				mock(FulfillmentPlatformActionService.class), taskMapper, taskOrderMapper, lineMapper,
				mock(WmsInventoryReservationMapper.class), mock(WmsFulfillmentItemMapper.class),
				mock(WmsLocationMapper.class), progress);

		WmsFulfillmentPickTask task = new WmsFulfillmentPickTask();
		task.setId(11L);
		task.setTaskStatus("PICKING");
		WmsFulfillmentPickTaskOrder taskOrder = new WmsFulfillmentPickTaskOrder();
		taskOrder.setId(12L);
		taskOrder.setTaskId(11L);
		taskOrder.setFulfillmentOrderId(1L);
		taskOrder.setOrderStatus("PENDING");
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(1L);
		order.setFulfillmentNo("FO-OZON-1");
		order.setFulfillmentStatus(FulfillmentStatus.PICKING);
		WmsFulfillmentPickTaskLine line = new WmsFulfillmentPickTaskLine();
		line.setId(21L);
		line.setVersion(0);

		when(taskMapper.selectById(11L)).thenReturn(task);
		when(taskOrderMapper.selectList(any())).thenReturn(Collections.singletonList(taskOrder));
		when(orderMapper.selectById(1L)).thenReturn(order);
		when(lineMapper.selectList(any())).thenReturn(Collections.singletonList(line));
		when(lineMapper.addPicked(21L, 1, 0)).thenReturn(1);
		when(lineMapper.selectCount(any())).thenReturn(0L);
		when(orderMapper.transit(1L, FulfillmentStatus.PICKING, FulfillmentStatus.WAITING_PACK)).thenReturn(1);

		FulfillmentPickScanDTO scan = new FulfillmentPickScanDTO();
		scan.setTaskId(11L);
		scan.setFulfillmentNo("FO-OZON-1");
		scan.setLocationCode("A1-01");
		scan.setWarehouseSkuCode("JHIN-SKU-A");
		scan.setQuantity(1);
		service.scan(scan);

		assertThat(taskOrder.getOrderStatus()).isEqualTo("WAITING_PACK");
		verify(taskMapper, never()).updateById(task);
		verify(progress).sync(eq(order), eq(FulfillmentStatus.WAITING_PACK));
	}

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
