package com.erp.admin.wms;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.erp.admin.wms.mapper.WmsLocationInventoryMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.dto.LogicalLocationCreateDTO;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsLocationInventory;
import com.erp.admin.wms.service.WmsLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WmsLogicalLocationServiceTest {

	private WmsLocationMapper locationMapper;

	private WmsLocationInventoryMapper inventoryMapper;

	private WmsLocationService service;

	@BeforeEach
	void setUp() {
		locationMapper = mock(WmsLocationMapper.class);
		inventoryMapper = mock(WmsLocationInventoryMapper.class);
		service = new WmsLocationService(inventoryMapper);
		ReflectionTestUtils.setField(service, "baseMapper", locationMapper);
	}

	@Test
	void create_keeps_the_requested_numeric_sequence_without_reordering_existing_locations() {
		LogicalLocationCreateDTO dto = location("A1-07", 7);
		when(locationMapper.countIncludingDeletedByCode(1L, "A1-07")).thenReturn(0L);
		when(locationMapper.countIncludingDeletedBySequence(1L, "A1", 7)).thenReturn(0L);
		when(locationMapper.insert(any(WmsLocation.class))).thenAnswer(invocation -> {
			invocation.<WmsLocation>getArgument(0).setId(99L);
			return 1;
		});

		assertThat(service.createLocation(dto)).isEqualTo(99L);
		ArgumentCaptor<WmsLocation> saved = ArgumentCaptor.forClass(WmsLocation.class);
		verify(locationMapper).insert(saved.capture());
		assertThat(saved.getValue().getRackNo()).isEqualTo("A1");
		assertThat(saved.getValue().getColumnNo()).isEqualTo(7);
		assertThat(saved.getValue().getLocationCode()).isEqualTo("A1-07");
	}

	@Test
	void create_rejects_a_code_that_was_used_before() {
		when(locationMapper.countIncludingDeletedByCode(1L, "A1-07")).thenReturn(1L);

		assertThatThrownBy(() -> service.createLocation(location("A1-07", 7)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("库位编号已存在");
		verify(locationMapper, never()).insert(any(WmsLocation.class));
	}

	@Test
	void delete_rejects_a_location_with_quantity_or_reservation() {
		WmsLocation location = new WmsLocation();
		location.setId(10L);
		when(locationMapper.selectLogicalByIdForUpdate(10L)).thenReturn(location);
		when(inventoryMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

		assertThatThrownBy(() -> service.deleteEmptyLocation(10L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("库位仍有库存或预占");
		verify(locationMapper, never()).deleteById(10L);
	}

	@Test
	void delete_allows_an_empty_location() {
		WmsLocation location = new WmsLocation();
		location.setId(10L);
		when(locationMapper.selectLogicalByIdForUpdate(10L)).thenReturn(location);
		when(inventoryMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
		when(locationMapper.deleteById(10L)).thenReturn(1);

		service.deleteEmptyLocation(10L);

		verify(locationMapper).deleteById(10L);
	}

	private LogicalLocationCreateDTO location(String code, int sequenceNo) {
		LogicalLocationCreateDTO dto = new LogicalLocationCreateDTO();
		dto.setWarehouseId(1L);
		dto.setZoneId(2L);
		dto.setRackNo("A1");
		dto.setSequenceNo(sequenceNo);
		dto.setLocationCode(code);
		dto.setLocationType("STANDARD");
		dto.setLengthMm(2000);
		dto.setWidthMm(1200);
		dto.setHeightMm(1800);
		dto.setMaxWeightKg(new java.math.BigDecimal("1200"));
		dto.setMaxSkuKinds(8);
		dto.setPublicShared(0);
		return dto;
	}

}
