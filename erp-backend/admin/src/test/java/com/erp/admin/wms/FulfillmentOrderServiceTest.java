package com.erp.admin.wms;

import java.util.Collections;

import com.erp.admin.wms.mapper.WmsFulfillmentItemMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.model.dto.FulfillmentCreateCommand;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import com.erp.admin.wms.service.FulfillmentOrderService;
import com.erp.admin.wms.service.FulfillmentReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FulfillmentOrderServiceTest {
	private WmsFulfillmentOrderMapper orderMapper;
	private WmsFulfillmentItemMapper itemMapper;
	private FulfillmentReservationService reservationService;
	private FulfillmentOrderService service;

	@BeforeEach
	void setUp() {
		orderMapper = mock(WmsFulfillmentOrderMapper.class);
		itemMapper = mock(WmsFulfillmentItemMapper.class);
		reservationService = mock(FulfillmentReservationService.class);
		service = new FulfillmentOrderService(orderMapper, itemMapper, reservationService);
	}

	@Test
	void creates_one_package_snapshot_and_reserves_before_waiting_shelf() {
		when(orderMapper.insert(any())).thenAnswer(invocation -> {
			WmsFulfillmentOrder order = invocation.getArgument(0);
			order.setId(91L);
			return 1;
		});
		when(itemMapper.insert(any())).thenReturn(1);
		when(orderMapper.transit(91L, FulfillmentStatus.DRAFT, FulfillmentStatus.WAITING_SHELF)).thenReturn(1);
		FulfillmentCreateCommand command = command();

		assertThat(service.createAndReserve(command)).isEqualTo(91L);
		verify(reservationService).reserve(org.mockito.ArgumentMatchers.eq(91L),
				org.mockito.ArgumentMatchers.eq(command), org.mockito.ArgumentMatchers.anyList());
		verify(orderMapper).transit(91L, FulfillmentStatus.DRAFT, FulfillmentStatus.WAITING_SHELF);
	}

	@Test
	void repeated_submission_returns_existing_order_without_reserving_again() {
		WmsFulfillmentOrder existing = new WmsFulfillmentOrder();
		existing.setId(77L);
		existing.setFulfillmentStatus(FulfillmentStatus.WAITING_PICK);
		when(orderMapper.selectBySource("OZON", 20L)).thenReturn(existing);

		assertThat(service.createAndReserve(command())).isEqualTo(77L);
		verify(orderMapper, never()).insert(any());
		verify(reservationService, never()).reserve(any(), any(), any());
	}

	@Test
	void cancelled_order_cannot_be_silently_resubmitted() {
		WmsFulfillmentOrder existing = new WmsFulfillmentOrder();
		existing.setId(77L);
		existing.setFulfillmentStatus(FulfillmentStatus.CANCELLED);
		when(orderMapper.selectBySource("OZON", 20L)).thenReturn(existing);

		assertThatThrownBy(() -> service.createAndReserve(command()))
				.hasMessageContaining("已取消");
		verify(reservationService, never()).reserve(any(), any(), any());
	}

	@Test
	void rejects_missing_outer_box_data_before_writing() {
		FulfillmentCreateCommand command = command();
		command.getItems().get(0).setOuterHeightMm(null);

		assertThatThrownBy(() -> service.createAndReserve(command)).hasMessageContaining("外箱高度");
		verify(orderMapper, never()).insert(any());
	}

	private FulfillmentCreateCommand command() {
		FulfillmentCreateCommand command = new FulfillmentCreateCommand();
		command.setTenantId(1L);
		command.setWmsTenantId(2L);
		command.setErpTenantId(3L);
		command.setWarehouseId(4L);
		command.setSourceType("ozon");
		command.setSourceOrderId(20L);
		command.setSourceOrderNo("OZ-20");
		FulfillmentCreateCommand.Item item = new FulfillmentCreateCommand.Item();
		item.setSkuCode("SKU-A");
		item.setWarehouseSkuCode("JHIN-SKU-A");
		item.setQuantity(2);
		item.setOuterLengthMm(100);
		item.setOuterWidthMm(200);
		item.setOuterHeightMm(300);
		item.setOuterGrossWeightG(1500);
		command.setItems(Collections.singletonList(item));
		return command;
	}
}
