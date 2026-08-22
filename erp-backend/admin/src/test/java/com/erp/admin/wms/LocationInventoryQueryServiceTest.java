package com.erp.admin.wms;

import java.math.BigDecimal;
import java.util.Collections;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.wms.mapper.WmsLocationInventoryMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsLocationInventory;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.vo.LocationInventoryGridVO;
import com.erp.admin.wms.service.LocationInventoryQueryService;
import com.erp.admin.wms.service.WarehouseService;
import com.erp.admin.wms.service.WmsZoneService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationInventoryQueryServiceTest {

	@Test
	void grid_reads_sku_dimensions_in_owner_context_and_returns_filter_metadata() {
		WarehouseService warehouseService = mock(WarehouseService.class);
		WmsLocationMapper locationMapper = mock(WmsLocationMapper.class);
		WmsLocationInventoryMapper inventoryMapper = mock(WmsLocationInventoryMapper.class);
		SkuMapper skuMapper = mock(SkuMapper.class);
		SysTenantMapper tenantMapper = mock(SysTenantMapper.class);
		WmsZoneService zoneService = mock(WmsZoneService.class);

		WmsLocation location = new WmsLocation();
		location.setId(1L);
		location.setWarehouseId(53L);
		location.setZoneId(28L);
		location.setRackNo("A1");
		location.setColumnNo(1);
		location.setLocationCode("A1-01");
		location.setLengthMm(1000);
		location.setWidthMm(1000);
		location.setHeightMm(1000);
		location.setMaxWeightKg(BigDecimal.valueOf(1000));

		WmsLocationInventory inventory = new WmsLocationInventory();
		inventory.setLocationId(1L);
		inventory.setWarehouseId(53L);
		inventory.setErpTenantId(6L);
		inventory.setSkuCode("SKU-A");
		inventory.setQuantity(1);
		inventory.setReservedQuantity(0);

		WmsZone zone = new WmsZone();
		zone.setId(28L);
		zone.setWarehouseId(53L);
		zone.setZoneName("标准区");
		zone.setZoneType("STANDARD");

		Sku sku = new Sku();
		sku.setSkuCode("SKU-A");
		sku.setOuterLengthMm(500);
		sku.setOuterWidthMm(1000);
		sku.setOuterHeightMm(1000);
		sku.setOuterGrossWeightG(1000);

		when(locationMapper.listByWarehouse(53L)).thenReturn(Collections.singletonList(location));
		when(inventoryMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(inventory));
		when(zoneService.listByWarehouse(53L)).thenReturn(Collections.singletonList(zone));
		when(skuMapper.selectBySkuCodes(Collections.singleton("SKU-A"))).thenAnswer(invocation ->
				Long.valueOf(6L).equals(TenantContext.getCurrentTenant())
						? Collections.singletonList(sku) : Collections.emptyList());
		LocationInventoryQueryService service = new LocationInventoryQueryService(warehouseService, locationMapper,
				inventoryMapper, skuMapper, tenantMapper, zoneService);

		TenantContext.setCurrentTenant(TenantContext.BLOCK_TENANT_ID);
		try {
			LocationInventoryGridVO result = service.grid(53L).get(0);

			assertThat(result.getUtilizationPercent()).isEqualByComparingTo("50.00");
			assertThat(result.getZoneId()).isEqualTo(28L);
			assertThat(result.getZoneName()).isEqualTo("标准区");
			assertThat(result.getZoneType()).isEqualTo("STANDARD");
			assertThat(result.getSkuCodes()).containsExactly("SKU-A");
			assertThat(TenantContext.getCurrentTenant()).isEqualTo(TenantContext.BLOCK_TENANT_ID);
		}
		finally {
			TenantContext.clear();
		}
	}

}
