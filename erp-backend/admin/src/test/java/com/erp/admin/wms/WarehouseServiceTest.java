package com.erp.admin.wms;

import com.erp.admin.wms.mapper.WarehouseMapper;
import com.erp.admin.wms.model.dto.WarehouseDTO;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.service.WarehouseService;
import com.erp.admin.wms.service.WmsZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WarehouseServiceTest {

	private WarehouseMapper warehouseMapper;

	private WmsZoneService zoneService;

	private WarehouseService service;

	@BeforeEach
	void setUp() {
		warehouseMapper = mock(WarehouseMapper.class);
		zoneService = mock(WmsZoneService.class);
		service = new WarehouseService();
		ReflectionTestUtils.setField(service, "baseMapper", warehouseMapper);
		ReflectionTestUtils.setField(service, "wmsZoneService", zoneService);
	}

	@Test
	void creating_a_manual_warehouse_initializes_all_four_default_zones() {
		when(warehouseMapper.countByWarehouseCode("CK-NEW", null)).thenReturn(0L);
		when(warehouseMapper.insert(any(Warehouse.class))).thenAnswer(invocation -> {
			invocation.<Warehouse>getArgument(0).setId(88L);
			return 1;
		});
		when(zoneService.initDefaultZones(88L)).thenReturn(4);

		WarehouseDTO dto = new WarehouseDTO();
		dto.setWarehouseCode("CK-NEW");
		dto.setWarehouseName("新仓库");
		dto.setWarehouseType("OWN");

		assertThat(service.createWarehouse(dto)).isTrue();
		verify(zoneService).initDefaultZones(88L);
	}

}
