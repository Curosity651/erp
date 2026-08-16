package com.erp.admin.wms;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.wms.mapper.WmsLocationInventoryMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.vo.LocationCapacityVO;
import com.erp.admin.wms.service.LocationCapacityService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationCapacityServiceTest {

	@Test
	void evaluate_reports_volume_and_rejects_overweight_placements() {
		WmsLocationMapper locationMapper = mock(WmsLocationMapper.class);
		WmsLocationInventoryMapper inventoryMapper = mock(WmsLocationInventoryMapper.class);
		SkuMapper skuMapper = mock(SkuMapper.class);
		when(locationMapper.selectById(1L)).thenReturn(location(2, "1000"));
		when(inventoryMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
		LocationCapacityService service = new LocationCapacityService(locationMapper, inventoryMapper, skuMapper);

		LocationCapacityVO result = service.evaluate(1L, Collections.singletonList(
				new LocationCapacityService.PlacementLine("SKU-A", 3, 400, 500, 500, 400_000L)));

		assertThat(result.getOccupiedVolumeMm3()).isEqualTo(300_000_000L);
		assertThat(result.getRemainingVolumeMm3()).isEqualTo(900_000_000L);
		assertThat(result.isWeightAllowed()).isFalse();
		assertThat(result.isSkuKindsAllowed()).isTrue();
	}

	@Test
	void evaluate_rejects_too_many_distinct_skus() {
		WmsLocationMapper locationMapper = mock(WmsLocationMapper.class);
		WmsLocationInventoryMapper inventoryMapper = mock(WmsLocationInventoryMapper.class);
		SkuMapper skuMapper = mock(SkuMapper.class);
		when(locationMapper.selectById(1L)).thenReturn(location(2, "5000"));
		when(inventoryMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
		LocationCapacityService service = new LocationCapacityService(locationMapper, inventoryMapper, skuMapper);

		LocationCapacityVO result = service.evaluate(1L, Arrays.asList(
				new LocationCapacityService.PlacementLine("SKU-A", 1, 100, 100, 100, 1000L),
				new LocationCapacityService.PlacementLine("SKU-B", 1, 100, 100, 100, 1000L),
				new LocationCapacityService.PlacementLine("SKU-C", 1, 100, 100, 100, 1000L)));

		assertThat(result.getSkuKindCount()).isEqualTo(3);
		assertThat(result.isSkuKindsAllowed()).isFalse();
	}

	private WmsLocation location(int maxKinds, String maxWeightKg) {
		WmsLocation location = new WmsLocation();
		location.setId(1L);
		location.setLengthMm(1200);
		location.setWidthMm(1000);
		location.setHeightMm(1000);
		location.setMaxWeightKg(new BigDecimal(maxWeightKg));
		location.setMaxSkuKinds(maxKinds);
		return location;
	}

}
