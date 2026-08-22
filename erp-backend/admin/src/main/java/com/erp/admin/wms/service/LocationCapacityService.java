package com.erp.admin.wms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.wms.mapper.WmsLocationInventoryMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsLocationInventory;
import com.erp.admin.wms.model.vo.LocationCapacityVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class LocationCapacityService {

	private final WmsLocationMapper locationMapper;

	private final WmsLocationInventoryMapper inventoryMapper;

	private final SkuMapper skuMapper;

	public LocationCapacityVO evaluate(Long locationId, List<PlacementLine> additions) {
		WmsLocation location = locationMapper.selectById(locationId);
		Assert.notNull(location, "库位不存在");
		validateLocationDimensions(location);
		List<WmsLocationInventory> inventory = inventoryMapper.selectList(
				Wrappers.<WmsLocationInventory>lambdaQuery()
					.eq(WmsLocationInventory::getLocationId, locationId)
					.gt(WmsLocationInventory::getQuantity, 0));
		if (inventory == null) {
			inventory = Collections.emptyList();
		}
		Map<Long, Set<String>> codesByTenant = new HashMap<>();
		for (WmsLocationInventory row : inventory) {
			if (value(row.getQuantity()) <= 0) {
				continue;
			}
			Assert.notNull(row.getErpTenantId(), "库位库存缺少货主归属：" + row.getSkuCode());
			codesByTenant.computeIfAbsent(row.getErpTenantId(), key -> new HashSet<>()).add(row.getSkuCode());
		}
		Map<Long, Map<String, Sku>> skuByTenant = new HashMap<>();
		for (Map.Entry<Long, Set<String>> entry : codesByTenant.entrySet()) {
			List<Sku> skus = TenantContext.runAs(entry.getKey(), () -> skuMapper.selectBySkuCodes(entry.getValue()));
			Map<String, Sku> byCode = new HashMap<>();
			if (skus != null) {
				for (Sku sku : skus) {
					byCode.putIfAbsent(sku.getSkuCode(), sku);
				}
			}
			skuByTenant.put(entry.getKey(), byCode);
		}

		long occupiedVolume = 0L;
		long occupiedWeight = 0L;
		Set<String> skuKinds = new HashSet<>();
		for (WmsLocationInventory row : inventory) {
			if (value(row.getQuantity()) <= 0) {
				continue;
			}
			Sku sku = skuByTenant.get(row.getErpTenantId()).get(row.getSkuCode());
			Assert.notNull(sku, "SKU不存在或无权访问：" + row.getSkuCode());
			PlacementLine line = fromSku(sku, row.getQuantity());
			occupiedVolume = Math.addExact(occupiedVolume, line.totalVolumeMm3());
			occupiedWeight = Math.addExact(occupiedWeight, line.totalWeightGrams());
			skuKinds.add(row.getSkuCode());
		}
		if (additions != null) {
			for (PlacementLine line : additions) {
				validateLine(line);
				if (line.getQuantity() == 0) {
					continue;
				}
				occupiedVolume = Math.addExact(occupiedVolume, line.totalVolumeMm3());
				occupiedWeight = Math.addExact(occupiedWeight, line.totalWeightGrams());
				skuKinds.add(line.getSkuCode());
			}
		}

		long capacityVolume = volume(location.getLengthMm(), location.getWidthMm(), location.getHeightMm());
		long maxWeight = toGrams(location.getMaxWeightKg(), "KG");
		int maxKinds = location.getMaxSkuKinds() == null ? 0 : location.getMaxSkuKinds();
		LocationCapacityVO result = new LocationCapacityVO();
		result.setLocationId(locationId);
		result.setCapacityVolumeMm3(capacityVolume);
		result.setOccupiedVolumeMm3(occupiedVolume);
		result.setRemainingVolumeMm3(Math.max(0L, capacityVolume - occupiedVolume));
		result.setOccupiedWeightGrams(occupiedWeight);
		result.setMaxWeightGrams(maxWeight);
		result.setSkuKindCount(skuKinds.size());
		result.setMaxSkuKinds(maxKinds);
		result.setVolumeAllowed(occupiedVolume <= capacityVolume);
		result.setWeightAllowed(maxWeight <= 0 || occupiedWeight <= maxWeight);
		result.setSkuKindsAllowed(maxKinds <= 0 || skuKinds.size() <= maxKinds);
		result.setUtilizationPercent(BigDecimal.valueOf(occupiedVolume)
				.multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(capacityVolume), 2, RoundingMode.HALF_UP));
		return result;
	}

	static PlacementLine fromSku(Sku sku, int quantity) {
		Assert.notNull(sku, "SKU不存在");
		Assert.isTrue(sku.getOuterLengthMm() != null && sku.getOuterLengthMm() > 0,
				"SKU外箱长度未维护");
		Assert.isTrue(sku.getOuterWidthMm() != null && sku.getOuterWidthMm() > 0,
				"SKU外箱宽度未维护");
		Assert.isTrue(sku.getOuterHeightMm() != null && sku.getOuterHeightMm() > 0,
				"SKU外箱高度未维护");
		Assert.isTrue(sku.getOuterGrossWeightG() != null && sku.getOuterGrossWeightG() > 0,
				"SKU单箱毛重未维护");
		return new PlacementLine(sku.getSkuCode(), quantity, sku.getOuterLengthMm(), sku.getOuterWidthMm(),
				sku.getOuterHeightMm(), sku.getOuterGrossWeightG());
	}

	static long volume(int lengthMm, int widthMm, int heightMm) {
		return Math.multiplyExact(Math.multiplyExact((long) lengthMm, widthMm), heightMm);
	}

	private static int toMillimetres(BigDecimal value, String unit) {
		Assert.isTrue(value != null && value.signum() > 0, "SKU外箱尺寸必须完整且大于0");
		BigDecimal factor;
		if ("MM".equalsIgnoreCase(unit)) {
			factor = BigDecimal.ONE;
		}
		else if ("CM".equalsIgnoreCase(unit)) {
			factor = BigDecimal.TEN;
		}
		else if ("M".equalsIgnoreCase(unit)) {
			factor = BigDecimal.valueOf(1000);
		}
		else {
			throw new IllegalArgumentException("SKU外箱尺寸单位必须为MM、CM或M");
		}
		return value.multiply(factor).setScale(0, RoundingMode.HALF_UP).intValueExact();
	}

	private static long toGrams(BigDecimal value, String unit) {
		Assert.isTrue(value != null && value.signum() > 0, "SKU单箱毛重必须完整且大于0");
		BigDecimal factor;
		if ("G".equalsIgnoreCase(unit)) {
			factor = BigDecimal.ONE;
		}
		else if ("KG".equalsIgnoreCase(unit)) {
			factor = BigDecimal.valueOf(1000);
		}
		else {
			throw new IllegalArgumentException("SKU重量单位必须为G或KG");
		}
		return value.multiply(factor).setScale(0, RoundingMode.HALF_UP).longValueExact();
	}

	private void validateLocationDimensions(WmsLocation location) {
		Assert.isTrue(location.getLengthMm() != null && location.getLengthMm() > 0, "库位长度未设置");
		Assert.isTrue(location.getWidthMm() != null && location.getWidthMm() > 0, "库位宽度未设置");
		Assert.isTrue(location.getHeightMm() != null && location.getHeightMm() > 0, "库位高度未设置");
		Assert.isTrue(location.getMaxWeightKg() != null && location.getMaxWeightKg().signum() > 0,
				"库位最大承重未设置");
	}

	private void validateLine(PlacementLine line) {
		Assert.notNull(line, "放置明细不能为空");
		Assert.hasText(line.getSkuCode(), "SKU不能为空");
		Assert.isTrue(line.getQuantity() >= 0, "放置数量不能小于0");
		Assert.isTrue(line.getLengthMm() > 0 && line.getWidthMm() > 0 && line.getHeightMm() > 0,
				"SKU外箱尺寸必须完整且大于0");
		Assert.isTrue(line.getUnitWeightGrams() > 0, "SKU单箱毛重必须大于0");
	}

	private int value(Integer value) {
		return value == null ? 0 : value;
	}

	@Data
	@AllArgsConstructor
	public static class PlacementLine {

		private String skuCode;

		private int quantity;

		private int lengthMm;

		private int widthMm;

		private int heightMm;

		private long unitWeightGrams;

		long totalVolumeMm3() {
			return Math.multiplyExact(volume(lengthMm, widthMm, heightMm), quantity);
		}

		long totalWeightGrams() {
			return Math.multiplyExact(unitWeightGrams, quantity);
		}
	}

}
