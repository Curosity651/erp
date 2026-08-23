package com.erp.admin.wms;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentItemMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskLineMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskOrderMapper;
import com.erp.admin.wms.mapper.WmsInventoryReservationMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.dto.FulfillmentPickScanDTO;
import com.erp.admin.wms.model.dto.FulfillmentPickExceptionDTO;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskLine;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentItem;
import com.erp.admin.wms.model.entity.WmsInventoryReservation;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import com.erp.admin.wms.model.vo.FulfillmentBatchResultVO;
import com.erp.admin.wms.service.FulfillmentPickingService;
import com.erp.admin.wms.service.FulfillmentPlatformActionService;
import com.erp.admin.wms.service.FulfillmentProgressService;
import com.erp.admin.wms.service.platform.PlatformActionResult;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import org.mockito.ArgumentCaptor;

class FulfillmentPickingServiceTest {
	@Test
	void claiming_pending_task_assigns_picker_and_starts_waiting_orders() {
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		WmsFulfillmentPickTaskMapper taskMapper = mock(WmsFulfillmentPickTaskMapper.class);
		WmsFulfillmentPickTaskOrderMapper taskOrderMapper = mock(WmsFulfillmentPickTaskOrderMapper.class);
		FulfillmentProgressService progress = mock(FulfillmentProgressService.class);
		FulfillmentPickingService service = new FulfillmentPickingService(orderMapper,
				mock(FulfillmentPlatformActionService.class), taskMapper, taskOrderMapper,
				mock(WmsFulfillmentPickTaskLineMapper.class),
				mock(WmsInventoryReservationMapper.class), mock(WmsFulfillmentItemMapper.class),
				mock(WmsLocationMapper.class), progress);

		WmsFulfillmentPickTask task = new WmsFulfillmentPickTask();
		task.setId(11L);
		task.setTaskStatus("PENDING");
		WmsFulfillmentPickTaskOrder taskOrder = new WmsFulfillmentPickTaskOrder();
		taskOrder.setTaskId(11L);
		taskOrder.setFulfillmentOrderId(1L);
		WmsFulfillmentOrder order = waitingPickOrder(1L, 10L, 5L, 6L);
		when(taskMapper.selectById(11L)).thenReturn(task);
		when(taskMapper.claim(eq(11L), eq(99L), any(LocalDateTime.class))).thenReturn(1);
		when(taskOrderMapper.selectList(any())).thenReturn(Collections.singletonList(taskOrder));
		when(orderMapper.selectForUpdate(1L)).thenReturn(order);
		when(orderMapper.transit(1L, FulfillmentStatus.WAITING_PICK,
				FulfillmentStatus.PICKING)).thenReturn(1);

		service.claimTask(11L, 99L);

		verify(taskMapper).claim(eq(11L), eq(99L), any(LocalDateTime.class));
		verify(orderMapper).transit(1L, FulfillmentStatus.WAITING_PICK,
				FulfillmentStatus.PICKING);
		verify(progress).sync(order, FulfillmentStatus.PICKING);
	}

	@Test
	void same_warehouse_task_allows_orders_from_different_owners_and_providers() {
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		WmsFulfillmentPickTaskMapper taskMapper = mock(WmsFulfillmentPickTaskMapper.class);
		WmsFulfillmentPickTaskOrderMapper taskOrderMapper = mock(WmsFulfillmentPickTaskOrderMapper.class);
		WmsFulfillmentPickTaskLineMapper lineMapper = mock(WmsFulfillmentPickTaskLineMapper.class);
		WmsInventoryReservationMapper reservationMapper = mock(WmsInventoryReservationMapper.class);
		WmsFulfillmentItemMapper itemMapper = mock(WmsFulfillmentItemMapper.class);
		WmsLocationMapper locationMapper = mock(WmsLocationMapper.class);
		FulfillmentPickingService service = new FulfillmentPickingService(orderMapper,
				mock(FulfillmentPlatformActionService.class), taskMapper, taskOrderMapper, lineMapper,
				reservationMapper, itemMapper, locationMapper, mock(FulfillmentProgressService.class));

		WmsFulfillmentOrder first = waitingPickOrder(1L, 10L, 5L, 6L);
		WmsFulfillmentOrder second = waitingPickOrder(2L, 10L, 8L, 9L);
		when(orderMapper.selectForUpdate(1L)).thenReturn(first);
		when(orderMapper.selectForUpdate(2L)).thenReturn(second);
		WmsFulfillmentPickTask[] createdTask = new WmsFulfillmentPickTask[1];
		when(taskMapper.insert(any(WmsFulfillmentPickTask.class))).thenAnswer(invocation -> {
			WmsFulfillmentPickTask task = invocation.getArgument(0);
			task.setId(50L);
			createdTask[0] = task;
			return 1;
		});
		WmsInventoryReservation firstReservation = reservation(101L, 201L, 301L);
		WmsInventoryReservation secondReservation = reservation(102L, 202L, 302L);
		when(reservationMapper.selectList(any())).thenReturn(
				Collections.singletonList(firstReservation), Collections.singletonList(secondReservation));
		when(itemMapper.selectById(201L)).thenReturn(item(201L, "OWNER-A-SKU"));
		when(itemMapper.selectById(202L)).thenReturn(item(202L, "OWNER-B-SKU"));
		when(locationMapper.selectById(301L)).thenReturn(location(301L, "A1-01"));
		when(locationMapper.selectById(302L)).thenReturn(location(302L, "A1-02"));
		when(orderMapper.transit(any(Long.class), eq(FulfillmentStatus.WAITING_PICK),
				eq(FulfillmentStatus.PICKING))).thenReturn(1);

		assertThatCode(() -> service.createTask(Arrays.asList(1L, 2L), 99L))
				.doesNotThrowAnyException();
		assertThat(createdTask[0].getWarehouseId()).isEqualTo(10L);
		assertThat(createdTask[0].getWmsTenantId()).isNull();
		assertThat(createdTask[0].getErpTenantId()).isNull();
		assertThat(createdTask[0].getTaskStatus()).isEqualTo("PENDING");
		assertThat(createdTask[0].getOperatorId()).isNull();
	}

	@Test
	void shelf_order_query_applies_time_owner_product_and_warehouse_filters() throws Exception {
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
				WmsFulfillmentOrder.class);
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		FulfillmentPickingService service = new FulfillmentPickingService(orderMapper,
				mock(FulfillmentPlatformActionService.class), mock(FulfillmentProgressService.class));
		LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
		LocalDateTime end = LocalDateTime.of(2026, 8, 31, 23, 59, 59);
		Method filteredQuery = Arrays.stream(FulfillmentPickingService.class.getMethods())
				.filter(method -> method.getName().equals("listShelfOrders"))
				.filter(method -> method.getParameterCount() == 5)
				.findFirst().orElse(null);

		assertThat(filteredQuery).as("filtered shelf-order query method").isNotNull();
		filteredQuery.invoke(service, start, end, 6L, 2L, 10L);

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
		verify(orderMapper).selectList(wrapperCaptor.capture());
		AbstractWrapper<?, ?, ?> wrapper = (AbstractWrapper<?, ?, ?>) wrapperCaptor.getValue();
		assertThat(wrapper.getSqlSegment()).contains("create_time", "erp_tenant_id",
				"logistics_product_id", "warehouse_id");
		assertThat(wrapper.getParamNameValuePairs().values()).contains(start, end, 6L, 2L, 10L);
	}

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
		task.setOperatorId(99L);
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
		service.scan(scan, 99L);

		assertThat(taskOrder.getOrderStatus()).isEqualTo("WAITING_LABEL");
		verify(taskMapper, never()).updateById(task);
		verify(progress).sync(eq(order), eq(FulfillmentStatus.WAITING_PACK));
	}

	@Test
	void picker_can_scan_any_pending_order_in_the_claimed_task() {
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		WmsFulfillmentPickTaskMapper taskMapper = mock(WmsFulfillmentPickTaskMapper.class);
		WmsFulfillmentPickTaskOrderMapper taskOrderMapper = mock(WmsFulfillmentPickTaskOrderMapper.class);
		WmsFulfillmentPickTaskLineMapper lineMapper = mock(WmsFulfillmentPickTaskLineMapper.class);
		FulfillmentPickingService service = new FulfillmentPickingService(orderMapper,
				mock(FulfillmentPlatformActionService.class), taskMapper, taskOrderMapper, lineMapper,
				mock(WmsInventoryReservationMapper.class), mock(WmsFulfillmentItemMapper.class),
				mock(WmsLocationMapper.class), mock(FulfillmentProgressService.class));

		WmsFulfillmentPickTask task = new WmsFulfillmentPickTask();
		task.setId(11L);
		task.setTaskStatus("PICKING");
		task.setOperatorId(99L);
		WmsFulfillmentPickTaskOrder first = taskOrder(31L, 11L, 1L);
		WmsFulfillmentPickTaskOrder second = taskOrder(32L, 11L, 2L);
		WmsFulfillmentOrder firstOrder = pickingOrder(1L, "FO-1");
		WmsFulfillmentOrder secondOrder = pickingOrder(2L, "FO-2");
		WmsFulfillmentPickTaskLine line = new WmsFulfillmentPickTaskLine();
		line.setId(41L);
		line.setVersion(0);
		when(taskMapper.selectById(11L)).thenReturn(task);
		when(taskOrderMapper.selectList(any())).thenReturn(Arrays.asList(first, second));
		when(orderMapper.selectById(1L)).thenReturn(firstOrder);
		when(orderMapper.selectById(2L)).thenReturn(secondOrder);
		when(lineMapper.selectList(any())).thenReturn(Collections.singletonList(line));
		when(lineMapper.addPicked(41L, 1, 0)).thenReturn(1);
		when(lineMapper.selectCount(any())).thenReturn(0L);
		when(orderMapper.transit(2L, FulfillmentStatus.PICKING,
				FulfillmentStatus.WAITING_PACK)).thenReturn(1);

		FulfillmentPickScanDTO scan = new FulfillmentPickScanDTO();
		scan.setTaskId(11L);
		scan.setFulfillmentNo("FO-2");
		scan.setLocationCode("A1-02");
		scan.setWarehouseSkuCode("JHIN-SKU-2");
		scan.setQuantity(1);
		service.scan(scan, 99L);

		assertThat(first.getOrderStatus()).isEqualTo("PENDING");
		assertThat(second.getOrderStatus()).isEqualTo("WAITING_LABEL");
		verify(orderMapper).transit(2L, FulfillmentStatus.PICKING,
				FulfillmentStatus.WAITING_PACK);
	}

	@Test
	void marking_order_exception_preserves_previous_stage_and_marks_task_partial() {
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		WmsFulfillmentPickTaskMapper taskMapper = mock(WmsFulfillmentPickTaskMapper.class);
		WmsFulfillmentPickTaskOrderMapper taskOrderMapper = mock(WmsFulfillmentPickTaskOrderMapper.class);
		FulfillmentPickingService service = new FulfillmentPickingService(orderMapper,
				mock(FulfillmentPlatformActionService.class), taskMapper, taskOrderMapper,
				mock(WmsFulfillmentPickTaskLineMapper.class),
				mock(WmsInventoryReservationMapper.class), mock(WmsFulfillmentItemMapper.class),
				mock(WmsLocationMapper.class), mock(FulfillmentProgressService.class));
		WmsFulfillmentPickTask task = new WmsFulfillmentPickTask();
		task.setId(11L);
		task.setTaskStatus("PICKING");
		task.setOperatorId(99L);
		WmsFulfillmentPickTaskOrder taskOrder = taskOrder(31L, 11L, 2L);
		taskOrder.setOrderStatus("PICKING");
		WmsFulfillmentOrder order = pickingOrder(2L, "FO-2");
		when(taskMapper.selectById(11L)).thenReturn(task);
		when(taskOrderMapper.selectOne(any())).thenReturn(taskOrder);
		when(orderMapper.selectForUpdate(2L)).thenReturn(order);
		when(orderMapper.transit(2L, FulfillmentStatus.PICKING,
				FulfillmentStatus.EXCEPTION)).thenReturn(1);
		FulfillmentPickExceptionDTO dto = new FulfillmentPickExceptionDTO();
		dto.setExceptionType("SHORTAGE");
		dto.setReason("库位实物不足");
		dto.setImageUrls(Arrays.asList("oss://evidence/1.jpg"));

		service.markException(11L, 2L, dto, 99L);

		assertThat(taskOrder.getPreviousOrderStatus()).isEqualTo("PICKING");
		assertThat(taskOrder.getPreviousFulfillmentStatus()).isEqualTo("PICKING");
		assertThat(taskOrder.getOrderStatus()).isEqualTo("EXCEPTION");
		assertThat(task.getTaskStatus()).isEqualTo("PARTIAL_EXCEPTION");
		verify(taskMapper).updateById(task);
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

	private WmsFulfillmentOrder waitingPickOrder(Long id, Long warehouseId,
			Long wmsTenantId, Long erpTenantId) {
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(id);
		order.setWarehouseId(warehouseId);
		order.setWmsTenantId(wmsTenantId);
		order.setErpTenantId(erpTenantId);
		order.setFulfillmentNo("FO-" + id);
		order.setFulfillmentStatus(FulfillmentStatus.WAITING_PICK);
		return order;
	}

	private WmsFulfillmentOrder pickingOrder(Long id, String fulfillmentNo) {
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(id);
		order.setFulfillmentNo(fulfillmentNo);
		order.setFulfillmentStatus(FulfillmentStatus.PICKING);
		return order;
	}

	private WmsFulfillmentPickTaskOrder taskOrder(Long id, Long taskId, Long fulfillmentOrderId) {
		WmsFulfillmentPickTaskOrder order = new WmsFulfillmentPickTaskOrder();
		order.setId(id);
		order.setTaskId(taskId);
		order.setFulfillmentOrderId(fulfillmentOrderId);
		order.setOrderStatus("PENDING");
		return order;
	}

	private WmsInventoryReservation reservation(Long id, Long itemId, Long locationId) {
		WmsInventoryReservation reservation = new WmsInventoryReservation();
		reservation.setId(id);
		reservation.setFulfillmentItemId(itemId);
		reservation.setInventoryId(id + 1000);
		reservation.setLocationId(locationId);
		reservation.setQuantity(1);
		return reservation;
	}

	private WmsFulfillmentItem item(Long id, String warehouseSkuCode) {
		WmsFulfillmentItem item = new WmsFulfillmentItem();
		item.setId(id);
		item.setSkuCode(warehouseSkuCode);
		item.setWarehouseSkuCode(warehouseSkuCode);
		return item;
	}

	private WmsLocation location(Long id, String locationCode) {
		WmsLocation location = new WmsLocation();
		location.setId(id);
		location.setLocationCode(locationCode);
		return location;
	}
}
