package com.erp.admin.wms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.wms.mapper.WmsLocationInventoryMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsLocationInventory;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.vo.LocationInventoryDetailVO;
import com.erp.admin.wms.model.vo.LocationInventoryGridVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class LocationInventoryQueryService {

	private final WarehouseService warehouseService;

	private final WmsLocationMapper locationMapper;

	private final WmsLocationInventoryMapper inventoryMapper;

	private final SkuMapper skuMapper;

	private final SysTenantMapper tenantMapper;

	private final WmsZoneService zoneService;

	public List<LocationInventoryGridVO> grid(Long warehouseId) {
		warehouseService.validateOperableOwnWarehouse(warehouseId);
		QueryData data = load(warehouseId);
		List<LocationInventoryGridVO> result = data.locations.stream()
			.map(location -> summarize(location, data.inventoryByLocation.getOrDefault(location.getId(), Collections.emptyList()),
					data.skuByTenant, data.zoneById.get(location.getZoneId())))
			.collect(Collectors.toList());
		result.sort(locationComparator());
		return result;
	}

	public Map<String, List<LocationInventoryGridVO>> tree(Long warehouseId) {
		return grid(warehouseId).stream().collect(Collectors.groupingBy(
				row -> row.getRackNo() == null ? "未分排" : row.getRackNo(), LinkedHashMap::new, Collectors.toList()));
	}

	public LocationInventoryDetailVO detail(Long locationId) {
		Assert.notNull(locationId, "库位不能为空");
		WmsLocation location = locationMapper.selectById(locationId);
		Assert.notNull(location, "库位不存在");
		warehouseService.validateOperableOwnWarehouse(location.getWarehouseId());
		List<WmsLocationInventory> inventory = inventoryMapper.selectList(
				Wrappers.<WmsLocationInventory>lambdaQuery()
					.eq(WmsLocationInventory::getLocationId, locationId)
					.gt(WmsLocationInventory::getQuantity, 0)
					.orderByAsc(WmsLocationInventory::getErpTenantId)
					.orderByAsc(WmsLocationInventory::getSkuCode)
					.orderByAsc(WmsLocationInventory::getQuality));
		Map<Long, Map<String, Sku>> skus = loadSkus(inventory);
		Map<Long, String> tenantNames = loadTenantNames(inventory);
		LocationInventoryDetailVO result = new LocationInventoryDetailVO();
		result.setLocation(summarize(location, inventory, skus, zoneService.getById(location.getZoneId())));
		result.setItems(inventory.stream().map(row -> detailLine(row, findSku(skus, row), tenantNames))
				.collect(Collectors.toList()));
		return result;
	}

	private QueryData load(Long warehouseId) {
		List<WmsLocation> locations = locationMapper.listByWarehouse(warehouseId);
		List<WmsLocationInventory> inventory = inventoryMapper.selectList(
				Wrappers.<WmsLocationInventory>lambdaQuery()
					.eq(WmsLocationInventory::getWarehouseId, warehouseId)
					.gt(WmsLocationInventory::getQuantity, 0));
		Map<Long, List<WmsLocationInventory>> byLocation = inventory.stream()
				.collect(Collectors.groupingBy(WmsLocationInventory::getLocationId));
		Map<Long, WmsZone> zoneById = zoneService.listByWarehouse(warehouseId).stream()
				.collect(Collectors.toMap(WmsZone::getId, Function.identity(), (left, right) -> left));
		return new QueryData(locations, byLocation, loadSkus(inventory), zoneById);
	}

	private Map<Long, Map<String, Sku>> loadSkus(List<WmsLocationInventory> inventory) {
		Map<Long, Set<String>> codesByTenant = inventory.stream()
				.filter(row -> row.getErpTenantId() != null && row.getSkuCode() != null)
				.collect(Collectors.groupingBy(WmsLocationInventory::getErpTenantId,
						Collectors.mapping(WmsLocationInventory::getSkuCode, Collectors.toSet())));
		if (codesByTenant.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Long, Map<String, Sku>> result = new LinkedHashMap<>();
		codesByTenant.forEach((tenantId, codes) -> {
			List<Sku> skus = TenantContext.runAs(tenantId, () -> skuMapper.selectBySkuCodes(codes));
			result.put(tenantId, skus == null ? Collections.emptyMap()
					: skus.stream().collect(Collectors.toMap(Sku::getSkuCode, Function.identity(),
							(left, right) -> left)));
		});
		return result;
	}

	private Sku findSku(Map<Long, Map<String, Sku>> skuByTenant, WmsLocationInventory inventory) {
		return skuByTenant.getOrDefault(inventory.getErpTenantId(), Collections.emptyMap())
				.get(inventory.getSkuCode());
	}

	private Map<Long, String> loadTenantNames(List<WmsLocationInventory> inventory) {
		Set<Long> ids = new HashSet<>();
		inventory.forEach(row -> {
			if (row.getWmsTenantId() != null) ids.add(row.getWmsTenantId());
			if (row.getErpTenantId() != null) ids.add(row.getErpTenantId());
		});
		if (ids.isEmpty()) {
			return Collections.emptyMap();
		}
		List<SysTenant> tenants = tenantMapper.selectBatchIds(ids);
		return tenants.stream().collect(Collectors.toMap(SysTenant::getId, SysTenant::getTenantName));
	}

	private LocationInventoryGridVO summarize(WmsLocation location, List<WmsLocationInventory> rows,
			Map<Long, Map<String, Sku>> skuByTenant, WmsZone zone) {
		LocationInventoryGridVO result = new LocationInventoryGridVO();
		result.setLocationId(location.getId());
		result.setWarehouseId(location.getWarehouseId());
		result.setRackNo(location.getRackNo());
		result.setSequenceNo(location.getColumnNo());
		result.setLocationCode(location.getLocationCode());
		result.setZoneId(location.getZoneId());
		result.setZoneName(zone == null ? null : zone.getZoneName());
		result.setZoneType(zone == null ? null : zone.getZoneType());
		result.setLocationType(location.getLocationType());
		result.setPublicShared(location.getPublicShared());
		long capacity = volume(location.getLengthMm(), location.getWidthMm(), location.getHeightMm());
		long maxWeight = location.getMaxWeightKg() == null ? 0L
				: location.getMaxWeightKg().multiply(BigDecimal.valueOf(1000)).longValue();
		long usedVolume = 0L;
		long usedWeight = 0L;
		int total = 0;
		int reserved = 0;
		boolean complete = capacity > 0;
		Set<String> kinds = new HashSet<>();
		for (WmsLocationInventory row : rows) {
			int quantity = value(row.getQuantity());
			total += quantity;
			reserved += value(row.getReservedQuantity());
			kinds.add(row.getSkuCode());
			Sku sku = findSku(skuByTenant, row);
			if (!validOuterBox(sku)) {
				complete = false;
				continue;
			}
			usedVolume = Math.addExact(usedVolume, Math.multiplyExact(boxVolume(sku), quantity));
			usedWeight = Math.addExact(usedWeight, Math.multiplyExact((long) sku.getOuterGrossWeightG(), quantity));
		}
		result.setCapacityVolumeMm3(capacity);
		result.setUsedVolumeMm3(usedVolume);
		result.setMaxWeightGrams(maxWeight);
		result.setUsedWeightGrams(usedWeight);
		result.setUtilizationPercent(percent(usedVolume, capacity));
		result.setSkuKindCount(kinds.size());
		result.setSkuCodes(kinds.stream().sorted().collect(Collectors.toList()));
		result.setTotalQuantity(total);
		result.setReservedQuantity(reserved);
		result.setAvailableQuantity(Math.max(0, total - reserved));
		result.setVolumeExceeded(capacity > 0 && usedVolume > capacity);
		result.setWeightExceeded(maxWeight > 0 && usedWeight > maxWeight);
		result.setCapacityDataComplete(complete);
		return result;
	}

	private LocationInventoryDetailVO.InventoryLineVO detailLine(WmsLocationInventory row, Sku sku,
			Map<Long, String> tenantNames) {
		LocationInventoryDetailVO.InventoryLineVO line = new LocationInventoryDetailVO.InventoryLineVO();
		line.setInventoryId(row.getId());
		line.setWmsTenantId(row.getWmsTenantId());
		line.setWmsTenantName(tenantNames.get(row.getWmsTenantId()));
		line.setErpTenantId(row.getErpTenantId());
		line.setOwnerName(tenantNames.get(row.getErpTenantId()));
		line.setSkuCode(row.getSkuCode());
		line.setSkuName(sku == null ? null : sku.getChineseName());
		line.setQuality(row.getQuality());
		line.setQuantity(value(row.getQuantity()));
		line.setReservedQuantity(value(row.getReservedQuantity()));
		line.setAvailableQuantity(Math.max(0, value(row.getQuantity()) - value(row.getReservedQuantity())));
		if (sku != null) {
			line.setOuterLengthMm(sku.getOuterLengthMm());
			line.setOuterWidthMm(sku.getOuterWidthMm());
			line.setOuterHeightMm(sku.getOuterHeightMm());
			line.setOuterGrossWeightG(sku.getOuterGrossWeightG());
		}
		return line;
	}

	private boolean validOuterBox(Sku sku) {
		return sku != null && positive(sku.getOuterLengthMm()) && positive(sku.getOuterWidthMm())
				&& positive(sku.getOuterHeightMm()) && positive(sku.getOuterGrossWeightG());
	}

	private boolean positive(Integer value) {
		return value != null && value > 0;
	}

	private long boxVolume(Sku sku) {
		return Math.multiplyExact(Math.multiplyExact((long) sku.getOuterLengthMm(), sku.getOuterWidthMm()),
				sku.getOuterHeightMm());
	}

	private long volume(Integer length, Integer width, Integer height) {
		if (!positive(length) || !positive(width) || !positive(height)) return 0L;
		return Math.multiplyExact(Math.multiplyExact((long) length, width), height);
	}

	private BigDecimal percent(long used, long capacity) {
		if (capacity <= 0) return BigDecimal.ZERO;
		return BigDecimal.valueOf(used).multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(capacity), 2, RoundingMode.HALF_UP);
	}

	private int value(Integer value) {
		return value == null ? 0 : value;
	}

	private Comparator<LocationInventoryGridVO> locationComparator() {
		return Comparator.comparing(LocationInventoryGridVO::getRackNo, this::naturalCompare)
				.thenComparing(row -> row.getSequenceNo() == null ? Integer.MAX_VALUE : row.getSequenceNo())
				.thenComparing(LocationInventoryGridVO::getLocationCode, Comparator.nullsLast(String::compareTo));
	}

	private int naturalCompare(String left, String right) {
		if (left == null) return right == null ? 0 : 1;
		if (right == null) return -1;
		String leftPrefix = left.replaceAll("\\d", "");
		String rightPrefix = right.replaceAll("\\d", "");
		int prefix = leftPrefix.compareToIgnoreCase(rightPrefix);
		if (prefix != 0) return prefix;
		return Integer.compare(number(left), number(right));
	}

	private int number(String value) {
		String digits = value.replaceAll("\\D", "");
		return digits.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(digits);
	}

	private static class QueryData {
		private final List<WmsLocation> locations;
		private final Map<Long, List<WmsLocationInventory>> inventoryByLocation;
		private final Map<Long, Map<String, Sku>> skuByTenant;
		private final Map<Long, WmsZone> zoneById;

		private QueryData(List<WmsLocation> locations,
				Map<Long, List<WmsLocationInventory>> inventoryByLocation,
				Map<Long, Map<String, Sku>> skuByTenant, Map<Long, WmsZone> zoneById) {
			this.locations = locations;
			this.inventoryByLocation = inventoryByLocation;
			this.skuByTenant = skuByTenant;
			this.zoneById = zoneById;
		}
	}

}
