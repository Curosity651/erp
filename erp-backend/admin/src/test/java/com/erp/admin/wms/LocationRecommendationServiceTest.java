package com.erp.admin.wms;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.wms.mapper.WmsLocationInventoryMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.vo.LocationRecommendationVO;
import com.erp.admin.wms.service.LocationCapacityService;
import com.erp.admin.wms.service.LocationRecommendationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationRecommendationServiceTest {

	@Test
	void recommend_uses_the_best_of_six_box_rotations_and_includes_public_temp_locations() {
		WmsLocationMapper locationMapper = mock(WmsLocationMapper.class);
		WmsLocationInventoryMapper inventoryMapper = mock(WmsLocationInventoryMapper.class);
		SkuMapper skuMapper = mock(SkuMapper.class);
		WmsLocation standard = location(1L, "A1-01", "STANDARD", 1200, 1000, 1000, 0);
		WmsLocation temp = location(2L, "TMP-01", "TEMP", 1000, 600, 400, 1);
		when(locationMapper.listByWarehouse(9L)).thenReturn(Arrays.asList(standard, temp));
		when(locationMapper.selectById(1L)).thenReturn(standard);
		when(locationMapper.selectById(2L)).thenReturn(temp);
		when(inventoryMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
		when(skuMapper.selectBySkuCode("SKU-A")).thenReturn(sku("SKU-A", 400, 500, 500, 50_000));
		LocationCapacityService capacityService = new LocationCapacityService(locationMapper, inventoryMapper, skuMapper);
		LocationRecommendationService service = new LocationRecommendationService(locationMapper, skuMapper,
				capacityService);

		List<LocationRecommendationVO> result = service.recommend(9L, 6L, "SKU-A", 20, "GOOD");

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getLocationCode()).isEqualTo("A1-01");
		assertThat(result.get(0).getRecommendedQuantity()).isEqualTo(12);
		assertThat(result.get(0).isWeightAllowed()).isTrue();
		assertThat(result.get(1).getLocationType()).isEqualTo("TEMP");
		assertThat(result.get(1).getRecommendedQuantity()).isEqualTo(2);
	}

	private WmsLocation location(Long id, String code, String type, int length, int width, int height,
			int publicShared) {
		WmsLocation location = new WmsLocation();
		location.setId(id);
		location.setWarehouseId(9L);
		location.setLocationCode(code);
		location.setLocationType(type);
		location.setLengthMm(length);
		location.setWidthMm(width);
		location.setHeightMm(height);
		location.setMaxWeightKg(new BigDecimal("1200"));
		location.setMaxSkuKinds(8);
		location.setPublicShared(publicShared);
		return location;
	}

	private Sku sku(String code, int length, int width, int height, int weightGrams) {
		Sku sku = new Sku();
		sku.setSkuCode(code);
		sku.setOuterLengthMm(length);
		sku.setOuterWidthMm(width);
		sku.setOuterHeightMm(height);
		sku.setOuterGrossWeightG(weightGrams);
		return sku;
	}

}
