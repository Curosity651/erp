package com.erp.admin.wms;

import java.util.Arrays;
import java.util.Collections;

import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import com.erp.admin.wms.model.vo.FulfillmentBatchResultVO;
import com.erp.admin.wms.model.vo.FulfillmentDispatchResultVO;
import com.erp.admin.wms.service.FulfillmentDispatchService;
import com.erp.admin.wms.service.FulfillmentPickingService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class FulfillmentDispatchServiceTest {

	@Test
	void dispatches_platform_successes_into_one_new_task_per_warehouse() {
		FulfillmentPickingService pickingService = mock(FulfillmentPickingService.class);
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		FulfillmentDispatchService service = new FulfillmentDispatchService(pickingService, orderMapper);
		FulfillmentBatchResultVO accepted = new FulfillmentBatchResultVO();
		accepted.addSuccess(1L);
		accepted.addSuccess(2L);
		accepted.addSuccess(3L);
		accepted.addFailure(4L, "平台确认失败");
		when(pickingService.accept(Arrays.asList(1L, 2L, 3L, 4L))).thenReturn(accepted);
		when(orderMapper.selectBatchIds(Arrays.asList(1L, 2L, 3L)))
				.thenReturn(Arrays.asList(order(1L, 10L), order(2L, 20L), order(3L, 10L)));
		when(pickingService.createTask(Arrays.asList(1L, 3L), 99L))
				.thenReturn(task(101L, "FPT-A", 10L, 2, 5));
		when(pickingService.createTask(Collections.singletonList(2L), 99L))
				.thenReturn(task(102L, "FPT-B", 20L, 1, 3));

		FulfillmentDispatchResultVO result = service.dispatch(Arrays.asList(1L, 2L, 3L, 4L), 99L);

		assertThat(result.getSuccessIds()).containsExactly(1L, 3L, 2L);
		assertThat(result.getFailures()).containsEntry(4L, "平台确认失败");
		assertThat(result.getTasks()).extracting("warehouseId").containsExactly(10L, 20L);
		verify(pickingService).createTask(Arrays.asList(1L, 3L), 99L);
		verify(pickingService).createTask(Collections.singletonList(2L), 99L);
	}

	@Test
	void one_warehouse_task_failure_does_not_block_another_warehouse() {
		FulfillmentPickingService pickingService = mock(FulfillmentPickingService.class);
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		FulfillmentDispatchService service = new FulfillmentDispatchService(pickingService, orderMapper);
		FulfillmentBatchResultVO accepted = new FulfillmentBatchResultVO();
		accepted.addSuccess(1L);
		accepted.addSuccess(2L);
		when(pickingService.accept(Arrays.asList(1L, 2L))).thenReturn(accepted);
		when(orderMapper.selectBatchIds(Arrays.asList(1L, 2L)))
				.thenReturn(Arrays.asList(order(1L, 10L), order(2L, 20L)));
		when(pickingService.createTask(Collections.singletonList(1L), 99L))
				.thenThrow(new IllegalStateException("库存预占不存在"));
		when(pickingService.createTask(Collections.singletonList(2L), 99L))
				.thenReturn(task(102L, "FPT-B", 20L, 1, 3));

		FulfillmentDispatchResultVO result = service.dispatch(Arrays.asList(1L, 2L), 99L);

		assertThat(result.getSuccessIds()).containsExactly(2L);
		assertThat(result.getFailures()).containsEntry(1L, "已下架但派单失败：库存预占不存在");
		assertThat(result.getTasks()).hasSize(1);
		assertThat(result.getTasks().get(0).getWarehouseId()).isEqualTo(20L);
	}

	@Test
	void redispatches_waiting_pick_order_without_repeating_platform_accept() {
		FulfillmentPickingService pickingService = mock(FulfillmentPickingService.class);
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		FulfillmentDispatchService service = new FulfillmentDispatchService(pickingService, orderMapper);
		WmsFulfillmentOrder pending = order(1L, 10L);
		pending.setFulfillmentStatus(com.erp.admin.wms.model.enums.FulfillmentStatus.WAITING_PICK);
		pending.setDispatchStatus("FAILED");
		when(orderMapper.selectBatchIds(Collections.singletonList(1L)))
				.thenReturn(Collections.singletonList(pending));
		when(pickingService.hasTaskAssociation(1L)).thenReturn(false);
		when(pickingService.createTask(Collections.singletonList(1L), 99L))
				.thenReturn(task(101L, "FPT-A", 10L, 1, 2));

		FulfillmentDispatchResultVO result = service.redispatch(Collections.singletonList(1L), 99L);

		assertThat(result.getSuccessIds()).containsExactly(1L);
		verify(pickingService, never()).accept(org.mockito.ArgumentMatchers.anyList());
		verify(pickingService).createTask(Collections.singletonList(1L), 99L);
	}

	@Test
	void redispatch_heals_status_when_task_was_created_before_status_writeback() {
		FulfillmentPickingService pickingService = mock(FulfillmentPickingService.class);
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		FulfillmentDispatchService service = new FulfillmentDispatchService(pickingService, orderMapper);
		WmsFulfillmentOrder pending = order(1L, 10L);
		pending.setFulfillmentStatus(com.erp.admin.wms.model.enums.FulfillmentStatus.WAITING_PICK);
		pending.setDispatchStatus("FAILED");
		when(orderMapper.selectBatchIds(Collections.singletonList(1L)))
				.thenReturn(Collections.singletonList(pending));
		when(pickingService.hasTaskAssociation(1L)).thenReturn(true);

		FulfillmentDispatchResultVO result = service.redispatch(Collections.singletonList(1L), 99L);

		assertThat(result.getSuccessIds()).containsExactly(1L);
		verify(pickingService, never()).accept(org.mockito.ArgumentMatchers.anyList());
		verify(pickingService, never()).createTask(org.mockito.ArgumentMatchers.anyList(),
				org.mockito.ArgumentMatchers.anyLong());
		verify(orderMapper).updateDispatchStatus(1L, "SUCCEEDED", null);
	}

	private WmsFulfillmentOrder order(Long id, Long warehouseId) {
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(id);
		order.setWarehouseId(warehouseId);
		return order;
	}

	private WmsFulfillmentPickTask task(Long id, String taskNo, Long warehouseId,
			Integer orderCount, Integer totalQuantity) {
		WmsFulfillmentPickTask task = new WmsFulfillmentPickTask();
		task.setId(id);
		task.setTaskNo(taskNo);
		task.setWarehouseId(warehouseId);
		task.setOrderCount(orderCount);
		task.setTotalQuantity(totalQuantity);
		return task;
	}
}
