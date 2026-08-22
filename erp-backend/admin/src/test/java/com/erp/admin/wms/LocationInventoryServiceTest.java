package com.erp.admin.wms;

import java.util.Arrays;
import java.util.Collections;

import com.erp.admin.wms.mapper.WmsInventoryReservationMapper;
import com.erp.admin.wms.mapper.WmsLocationInventoryMapper;
import com.erp.admin.wms.model.dto.InventoryReservationRequest;
import com.erp.admin.wms.model.dto.LocationInventoryKey;
import com.erp.admin.wms.model.entity.WmsLocationInventory;
import com.erp.admin.wms.service.LocationInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocationInventoryServiceTest {

	private WmsLocationInventoryMapper inventoryMapper;

	private WmsInventoryReservationMapper reservationMapper;

	private LocationInventoryService service;

	@BeforeEach
	void setUp() {
		inventoryMapper = mock(WmsLocationInventoryMapper.class);
		reservationMapper = mock(WmsInventoryReservationMapper.class);
		service = new LocationInventoryService(inventoryMapper, reservationMapper);
	}

	@Test
	void increase_merges_inventory_with_the_exact_same_owner_dimension() {
		LocationInventoryKey key = key(6L, 2L);
		WmsLocationInventory existing = inventory(20L, 6L, 2L, 10, 2, 3);
		when(inventoryMapper.selectByKeyForUpdate(key)).thenReturn(existing);
		when(inventoryMapper.increaseQuantity(20L, 5, 3)).thenReturn(1);

		service.increase(key, 5);

		verify(inventoryMapper).increaseQuantity(20L, 5, 3);
		verify(inventoryMapper, never()).insert(any(WmsLocationInventory.class));
	}

	@Test
	void increase_never_merges_a_different_owner() {
		LocationInventoryKey key = key(7L, 2L);
		when(inventoryMapper.selectByKeyForUpdate(key)).thenReturn(null);
		when(inventoryMapper.insert(any(WmsLocationInventory.class))).thenReturn(1);

		service.increase(key, 4);

		ArgumentCaptor<WmsLocationInventory> inserted = ArgumentCaptor.forClass(WmsLocationInventory.class);
		verify(inventoryMapper).insert(inserted.capture());
		assertThat(inserted.getValue().getErpTenantId()).isEqualTo(7L);
		assertThat(inserted.getValue().getLocationId()).isEqualTo(2L);
		assertThat(inserted.getValue().getQuantity()).isEqualTo(4);
		assertThat(inserted.getValue().getReservedQuantity()).isZero();
	}

	@Test
	void increase_rejects_non_positive_quantity() {
		assertThatThrownBy(() -> service.increase(key(6L, 2L), 0))
				.hasMessageContaining("数量必须大于0");
		verify(inventoryMapper, never()).insert(any(WmsLocationInventory.class));
	}

	@Test
	void reserve_uses_versioned_conditional_updates_in_inventory_id_order() {
		WmsLocationInventory first = inventory(10L, 6L, 2L, 3, 0, 1);
		WmsLocationInventory second = inventory(20L, 6L, 3L, 5, 1, 4);
		when(inventoryMapper.selectOwnedAvailableForUpdate(1L, 5L, 9L, 6L, "SKU-A", "GOOD"))
				.thenReturn(Arrays.asList(first, second));
		when(inventoryMapper.reserveQuantity(10L, 3, 1)).thenReturn(1);
		when(inventoryMapper.reserveQuantity(20L, 2, 4)).thenReturn(1);
		when(reservationMapper.insert(any())).thenReturn(1);

		assertThat(service.reserve(request(100L, 5))).containsExactly(10L, 20L);
		verify(inventoryMapper).reserveQuantity(10L, 3, 1);
		verify(inventoryMapper).reserveQuantity(20L, 2, 4);
		ArgumentCaptor<com.erp.admin.wms.model.entity.WmsInventoryReservation> reservations =
				ArgumentCaptor.forClass(com.erp.admin.wms.model.entity.WmsInventoryReservation.class);
		verify(reservationMapper, org.mockito.Mockito.times(2)).insert(reservations.capture());
		assertThat(reservations.getAllValues())
				.extracting(com.erp.admin.wms.model.entity.WmsInventoryReservation::getFulfillmentItemId)
				.containsExactly(33L, 33L);
		assertThat(reservations.getAllValues())
				.extracting(com.erp.admin.wms.model.entity.WmsInventoryReservation::getLocationId)
				.containsExactly(2L, 3L);
	}

	@Test
	void reserve_rejects_shortage_without_silently_over_reserving() {
		when(inventoryMapper.selectOwnedAvailableForUpdate(1L, 5L, 9L, 6L, "SKU-A", "GOOD"))
				.thenReturn(Collections.singletonList(inventory(10L, 6L, 2L, 2, 0, 1)));
		when(inventoryMapper.reserveQuantity(10L, 2, 1)).thenReturn(1);
		when(reservationMapper.insert(any())).thenReturn(1);

		assertThatThrownBy(() -> service.reserve(request(100L, 3)))
				.hasMessageContaining("库存不足");
	}

	@Test
	void move_rejects_quantity_that_includes_reserved_stock() {
		WmsLocationInventory source = inventory(10L, 6L, 2L, 10, 4, 1);
		when(inventoryMapper.selectForUpdate(10L)).thenReturn(source);

		assertThatThrownBy(() -> service.move(10L, 3L, 7))
				.hasMessageContaining("可移动数量不足");
		verify(inventoryMapper, never()).decreaseAvailableQuantity(anyLong(), anyInt(), anyInt());
	}

	private LocationInventoryKey key(Long ownerId, Long locationId) {
		LocationInventoryKey key = new LocationInventoryKey();
		key.setTenantId(1L);
		key.setWmsTenantId(5L);
		key.setErpTenantId(ownerId);
		key.setWarehouseId(9L);
		key.setLocationId(locationId);
		key.setSkuCode("SKU-A");
		key.setQuality("GOOD");
		return key;
	}

	private InventoryReservationRequest request(Long fulfillmentId, int quantity) {
		InventoryReservationRequest request = new InventoryReservationRequest();
		request.setFulfillmentOrderId(fulfillmentId);
		org.springframework.test.util.ReflectionTestUtils.setField(request, "fulfillmentItemId", 33L);
		request.setTenantId(1L);
		request.setWmsTenantId(5L);
		request.setErpTenantId(6L);
		request.setWarehouseId(9L);
		request.setSkuCode("SKU-A");
		request.setQuality("GOOD");
		request.setQuantity(quantity);
		return request;
	}

	private WmsLocationInventory inventory(Long id, Long ownerId, Long locationId, int quantity,
			int reserved, int version) {
		WmsLocationInventory row = new WmsLocationInventory();
		row.setId(id);
		row.setTenantId(1L);
		row.setWmsTenantId(5L);
		row.setErpTenantId(ownerId);
		row.setWarehouseId(9L);
		row.setLocationId(locationId);
		row.setSkuCode("SKU-A");
		row.setQuality("GOOD");
		row.setQuantity(quantity);
		row.setReservedQuantity(reserved);
		row.setVersion(version);
		return row;
	}

}
