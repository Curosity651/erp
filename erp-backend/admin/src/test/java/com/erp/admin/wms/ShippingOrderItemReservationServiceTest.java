package com.erp.admin.wms;

import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.mapper.ShippingOrderItemMapper;
import com.erp.admin.wms.model.entity.PurchaseInboundOrderItem;
import com.erp.admin.wms.model.entity.ShippingOrderItem;
import com.erp.admin.wms.service.ShippingOrderItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShippingOrderItemReservationServiceTest {

	private ShippingOrderItemMapper mapper;

	private ShippingOrderItemService service;

	@BeforeEach
	void setUp() {
		mapper = mock(ShippingOrderItemMapper.class);
		service = new ShippingOrderItemService(mock(SkuBriefService.class));
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
	}

	@Test
	void submit_reserves_expected_quantity() {
		ShippingOrderItem shippingItem = shippingItem();
		PurchaseInboundOrderItem inboundItem = inboundItem(80, 0);
		when(mapper.selectById(90027L)).thenReturn(shippingItem);
		when(mapper.reserveInboundQuantity(90027L, 80)).thenReturn(1);

		service.reserveInboundQuantities(9004L, Collections.singletonList(inboundItem));

		verify(mapper).reserveInboundQuantity(90027L, 80);
	}

	@Test
	void submit_rejects_item_from_another_shipping_order() {
		ShippingOrderItem shippingItem = shippingItem();
		shippingItem.setShippingOrderId(9999L);
		when(mapper.selectById(90027L)).thenReturn(shippingItem);

		assertThatThrownBy(() -> service.reserveInboundQuantities(
				9004L, Collections.singletonList(inboundItem(80, 0))))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("不匹配");

		verify(mapper, never()).reserveInboundQuantity(90027L, 80);
	}

	@Test
	void receive_releases_expected_and_books_actual_quantity() {
		PurchaseInboundOrderItem inboundItem = inboundItem(80, 60);
		when(mapper.settleInboundReservation(90027L, 80, 60)).thenReturn(1);

		service.settleInboundReservations(Collections.singletonList(inboundItem));

		verify(mapper).settleInboundReservation(90027L, 80, 60);
	}

	private ShippingOrderItem shippingItem() {
		ShippingOrderItem item = new ShippingOrderItem();
		item.setId(90027L);
		item.setShippingOrderId(9004L);
		item.setPurchaseOrderId(7001L);
		item.setPurchaseOrderItemId(70011L);
		item.setSkuCode("X001-BL-LED");
		item.setQuantity(80);
		item.setReceivedQuantity(0);
		item.setInboundReservedQuantity(0);
		return item;
	}

	private PurchaseInboundOrderItem inboundItem(int expected, int actual) {
		PurchaseInboundOrderItem item = new PurchaseInboundOrderItem();
		item.setShippingOrderItemId(90027L);
		item.setPurchaseOrderId(7001L);
		item.setPurchaseOrderItemId(70011L);
		item.setSkuCode("X001-BL-LED");
		item.setExpectedQuantity(expected);
		item.setActualQuantity(actual);
		return item;
	}
}
