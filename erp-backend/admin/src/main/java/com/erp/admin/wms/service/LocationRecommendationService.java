package com.erp.admin.wms.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.vo.LocationCapacityVO;
import com.erp.admin.wms.model.vo.LocationRecommendationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class LocationRecommendationService {

	private final WmsLocationMapper locationMapper;

	private final SkuMapper skuMapper;

	private final LocationCapacityService capacityService;

	public List<LocationRecommendationVO> recommend(Long warehouseId, Long erpTenantId, String skuCode,
			int quantity, String quality) {
		Assert.notNull(warehouseId, "仓库不能为空");
		Assert.notNull(erpTenantId, "货主不能为空");
		Assert.hasText(skuCode, "SKU不能为空");
		Assert.hasText(quality, "品质不能为空");
		Assert.isTrue(quantity > 0, "待上架数量必须大于0");
		Sku sku = TenantContext.runAs(erpTenantId, () -> skuMapper.selectBySkuCode(skuCode));
		Assert.notNull(sku, "SKU不存在或无权访问：" + skuCode);
		LocationCapacityService.PlacementLine unit = LocationCapacityService.fromSku(sku, 1);
		List<LocationRecommendationVO> result = new ArrayList<>();
		for (WmsLocation location : locationMapper.listByWarehouse(warehouseId)) {
			if (!hasDimensions(location)) {
				continue;
			}
			LocationCapacityVO current = capacityService.evaluate(location.getId(), Collections.emptyList());
			LocationCapacityVO withOne = capacityService.evaluate(location.getId(), Collections.singletonList(unit));
			int byGeometry = maxByRotation(location, unit);
			int byVolume = divide(current.getRemainingVolumeMm3(), unitVolume(unit));
			long remainingWeight = Math.max(0L,
					current.getMaxWeightGrams() - current.getOccupiedWeightGrams());
			int byWeight = divide(remainingWeight, unit.getUnitWeightGrams());
			int recommended = withOne.isSkuKindsAllowed()
					? Math.min(quantity, Math.min(byGeometry, Math.min(byVolume, byWeight))) : 0;
			LocationRecommendationVO vo = new LocationRecommendationVO();
			vo.setLocationId(location.getId());
			vo.setLocationCode(location.getLocationCode());
			vo.setRackNo(location.getRackNo());
			vo.setSequenceNo(location.getColumnNo());
			vo.setLocationType(location.getLocationType());
			vo.setPublicShared(location.getPublicShared());
			vo.setRecommendedQuantity(recommended);
			vo.setMaxByGeometry(byGeometry);
			vo.setMaxByVolume(byVolume);
			vo.setMaxByWeight(byWeight);
			vo.setRemainingVolumeMm3(current.getRemainingVolumeMm3());
			vo.setWeightAllowed(byWeight > 0);
			vo.setSkuKindsAllowed(withOne.isSkuKindsAllowed());
			result.add(vo);
		}
		result.sort(Comparator.comparingInt(LocationRecommendationVO::getRecommendedQuantity).reversed()
				.thenComparing(LocationRecommendationVO::getLocationCode,
						Comparator.nullsLast(String::compareTo)));
		return result;
	}

	private int maxByRotation(WmsLocation location, LocationCapacityService.PlacementLine item) {
		int[][] rotations = {
				{ item.getLengthMm(), item.getWidthMm(), item.getHeightMm() },
				{ item.getLengthMm(), item.getHeightMm(), item.getWidthMm() },
				{ item.getWidthMm(), item.getLengthMm(), item.getHeightMm() },
				{ item.getWidthMm(), item.getHeightMm(), item.getLengthMm() },
				{ item.getHeightMm(), item.getLengthMm(), item.getWidthMm() },
				{ item.getHeightMm(), item.getWidthMm(), item.getLengthMm() }
		};
		long max = 0;
		for (int[] rotation : rotations) {
			long count = (long) (location.getLengthMm() / rotation[0])
					* (location.getWidthMm() / rotation[1])
					* (location.getHeightMm() / rotation[2]);
			max = Math.max(max, count);
		}
		return max > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) max;
	}

	private long unitVolume(LocationCapacityService.PlacementLine item) {
		return LocationCapacityService.volume(item.getLengthMm(), item.getWidthMm(), item.getHeightMm());
	}

	private int divide(long numerator, long denominator) {
		if (numerator <= 0 || denominator <= 0) {
			return 0;
		}
		long result = numerator / denominator;
		return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
	}

	private boolean hasDimensions(WmsLocation location) {
		return location.getLengthMm() != null && location.getLengthMm() > 0
				&& location.getWidthMm() != null && location.getWidthMm() > 0
				&& location.getHeightMm() != null && location.getHeightMm() > 0
				&& location.getMaxWeightKg() != null && location.getMaxWeightKg().signum() > 0;
	}

}
