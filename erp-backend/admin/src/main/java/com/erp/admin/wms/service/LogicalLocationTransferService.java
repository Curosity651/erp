package com.erp.admin.wms.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.LocationTransferOrderMapper;
import com.erp.admin.wms.mapper.WmsLocationInventoryMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.dto.LogicalLocationTransferCreateDTO;
import com.erp.admin.wms.model.entity.LocationTransferItem;
import com.erp.admin.wms.model.entity.LocationTransferOrder;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsLocationInventory;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.enums.LocationTransferReason;
import com.erp.admin.wms.model.enums.LocationTransferStatus;
import com.erp.admin.wms.model.vo.LocationCapacityVO;
import com.erp.admin.wms.model.vo.LogicalTransferLocationVO;
import com.erp.admin.wms.model.vo.LogicalTransferSourceVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LogicalLocationTransferService {

	private static final String GOOD = "GOOD";

	private static final String DEFECTIVE = "DEFECTIVE";

	private static final String DAMAGED = "DAMAGED";

	private final LocationTransferOrderMapper orderMapper;

	private final LocationTransferItemService itemService;

	private final WmsLocationInventoryMapper inventoryMapper;

	private final WmsLocationMapper locationMapper;

	private final WmsLocationService locationService;

	private final WmsZoneService zoneService;

	private final WmsRackAssignmentService rackAssignmentService;

	private final LocationInventoryService inventoryService;

	private final LocationCapacityService capacityService;

	private final SkuMapper skuMapper;

	private final SysTenantMapper tenantMapper;

	private final WarehouseService warehouseService;

	private final WarehouseSkuCodeService warehouseSkuCodeService;

	private final TenantIdentityService tenantIdentityService;

	private final PrincipalAttributeAccessor principalAccessor;

	public List<LogicalTransferSourceVO> listSources(Long warehouseId, Long locationId, Long erpTenantId,
			String skuKeyword) {
		assertPlatform();
		warehouseService.validateOperableOwnWarehouse(warehouseId);
		List<WmsLocationInventory> rows = inventoryMapper.selectList(
				Wrappers.<WmsLocationInventory>lambdaQuery()
					.eq(WmsLocationInventory::getWarehouseId, warehouseId)
					.eq(locationId != null, WmsLocationInventory::getLocationId, locationId)
					.eq(erpTenantId != null, WmsLocationInventory::getErpTenantId, erpTenantId)
					.like(StringUtils.hasText(skuKeyword), WmsLocationInventory::getSkuCode, skuKeyword)
					.apply("quantity > reserved_quantity")
					.notIn(WmsLocationInventory::getQuality, DEFECTIVE, DAMAGED)
					.orderByAsc(WmsLocationInventory::getLocationId, WmsLocationInventory::getSkuCode,
							WmsLocationInventory::getId));
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyList();
		}
		Map<Long, WmsLocation> locations = locationMap(warehouseId);
		Map<Long, WmsZone> zones = zoneMap(warehouseId);
		Map<Long, SysTenant> owners = tenantMapper.selectBatchIds(rows.stream()
				.map(WmsLocationInventory::getErpTenantId).collect(Collectors.toSet())).stream()
				.collect(Collectors.toMap(SysTenant::getId, Function.identity(), (left, right) -> left));
		List<LogicalTransferSourceVO> result = new ArrayList<>();
		for (WmsLocationInventory row : rows) {
			WmsLocation location = locations.get(row.getLocationId());
			if (location == null) {
				continue;
			}
			WmsZone zone = zones.get(location.getZoneId());
			Set<String> allowedRacks = rackAssignmentService.activeRackNos(warehouseId, row.getWmsTenantId());
			if (!hasTargetAccess(location, zoneType(location, zone), allowedRacks)) {
				continue;
			}
			LogicalTransferSourceVO vo = new LogicalTransferSourceVO();
			vo.setInventoryId(row.getId());
			vo.setWarehouseId(row.getWarehouseId());
			vo.setWmsTenantId(row.getWmsTenantId());
			vo.setErpTenantId(row.getErpTenantId());
			SysTenant owner = owners.get(row.getErpTenantId());
			vo.setOwnerName(owner == null ? null : owner.getTenantName());
			vo.setSkuCode(row.getSkuCode());
			vo.setWarehouseSkuCode(warehouseSkuCodeService.build(row.getErpTenantId(), row.getSkuCode()));
			vo.setQuality(row.getQuality());
			vo.setQuantity(value(row.getQuantity()));
			vo.setReservedQuantity(value(row.getReservedQuantity()));
			vo.setAvailableQuantity(value(row.getQuantity()) - value(row.getReservedQuantity()));
			vo.setLocationId(location.getId());
			vo.setLocationCode(location.getLocationCode());
			vo.setRackNo(location.getRackNo());
			vo.setLocationType(location.getLocationType());
			vo.setZoneName(zone == null ? null : zone.getZoneName());
			vo.setZoneType(zoneType(location, zone));
			result.add(vo);
		}
		result.sort(Comparator.comparing(LogicalTransferSourceVO::getRackNo,
				Comparator.nullsLast(WmsRackAssignmentService::compareRackNo))
				.thenComparing(LogicalTransferSourceVO::getLocationCode,
						Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
				.thenComparing(LogicalTransferSourceVO::getSkuCode));
		return result;
	}

	public List<LogicalTransferLocationVO> listTargets(Long warehouseId, Long erpTenantId) {
		assertPlatform();
		warehouseService.validateOperableOwnWarehouse(warehouseId);
		SysTenant owner = requireOwner(erpTenantId);
		Set<String> allowedRacks = rackAssignmentService.activeRackNos(warehouseId, owner.getParentWmsTenantId());
		Map<Long, WmsZone> zones = zoneMap(warehouseId);
		List<LogicalTransferLocationVO> result = new ArrayList<>();
		for (WmsLocation location : locationService.listByWarehouse(warehouseId)) {
			WmsZone zone = zones.get(location.getZoneId());
			String type = zoneType(location, zone);
			if (DEFECTIVE.equalsIgnoreCase(type) || !hasTargetAccess(location, type, allowedRacks)) {
				continue;
			}
			LogicalTransferLocationVO vo = new LogicalTransferLocationVO();
			vo.setLocationId(location.getId());
			vo.setLocationCode(location.getLocationCode());
			vo.setRackNo(location.getRackNo());
			vo.setColumnNo(location.getColumnNo());
			vo.setLocationType(location.getLocationType());
			vo.setZoneName(zone == null ? null : zone.getZoneName());
			vo.setZoneType(type);
			vo.setPublicShared(location.getPublicShared());
			vo.setUtilizationPercent(capacityService.evaluate(location.getId(), Collections.emptyList())
					.getUtilizationPercent());
			result.add(vo);
		}
		result.sort(Comparator.comparing(LogicalTransferLocationVO::getRackNo,
				Comparator.nullsLast(WmsRackAssignmentService::compareRackNo))
				.thenComparing(LogicalTransferLocationVO::getColumnNo, Comparator.nullsLast(Integer::compareTo))
				.thenComparing(LogicalTransferLocationVO::getLocationCode,
						Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
		return result;
	}

	@Transactional(rollbackFor = Exception.class)
	public Long create(LogicalLocationTransferCreateDTO dto) {
		assertPlatform();
		Assert.notNull(dto, "调整单不能为空");
		Assert.notEmpty(dto.getItems(), "调整明细不能为空");
		Assert.isTrue(LocationTransferReason.isManualReason(dto.getReasonCode()), "请选择有效的调整原因");
		if (LocationTransferReason.OTHER.name().equals(dto.getReasonCode())) {
			Assert.hasText(dto.getReason(), "选择“其他”时必须填写原因说明");
		}
		warehouseService.validateOperableOwnWarehouse(dto.getWarehouseId());
		SysTenant owner = requireOwner(dto.getErpTenantId());
		Long wmsTenantId = owner.getParentWmsTenantId();
		Assert.notNull(wmsTenantId, "货主未绑定WMS服务商");
		List<LogicalLocationTransferCreateDTO.Item> lines = mergeLines(dto.getItems());
		Map<Long, WmsLocationInventory> sources = sourceMap(lines);
		Map<Long, WmsLocation> locations = locationMap(dto.getWarehouseId());
		Map<Long, WmsZone> zones = zoneMap(dto.getWarehouseId());
		Set<String> allowedRacks = rackAssignmentService.activeRackNos(dto.getWarehouseId(), wmsTenantId);
		validateLines(dto, lines, sources, locations, zones, allowedRacks);
		validateCapacity(lines, sources, locations);

		LocationTransferOrder order = new LocationTransferOrder();
		order.setWarehouseId(dto.getWarehouseId());
		order.setWmsTenantId(wmsTenantId);
		order.setErpTenantId(dto.getErpTenantId());
		order.setOrderStatus(LocationTransferStatus.PENDING.name());
		order.setSourceType("MANUAL");
		order.setReasonCode(dto.getReasonCode());
		order.setReason(dto.getReason());
		order.setRemark(dto.getRemark());
		order.setCreateBy(currentUserId());
		saveOrderWithRetry(order);

		List<LocationTransferItem> items = new ArrayList<>();
		for (LogicalLocationTransferCreateDTO.Item line : lines) {
			WmsLocationInventory source = sources.get(line.getSourceInventoryId());
			WmsLocation sourceLocation = locations.get(source.getLocationId());
			WmsLocation target = locations.get(line.getTargetLocationId());
			LocationTransferItem item = new LocationTransferItem();
			item.setTransferOrderId(order.getId());
			item.setErpTenantId(source.getErpTenantId());
			item.setSkuCode(source.getSkuCode());
			item.setSourceInventoryId(source.getId());
			item.setSourceLocationCode(sourceLocation.getLocationCode());
			item.setSourceQuality(source.getQuality());
			item.setMoveMode("LOGICAL");
			item.setTargetLocationId(target.getId());
			item.setTargetLocationCode(target.getLocationCode());
			item.setTargetZoneId(target.getZoneId());
			item.setQuantity(line.getQuantity());
			item.setRemark(line.getRemark());
			items.add(item);
		}
		Assert.isTrue(itemService.saveBatch(items), "库位调整明细保存失败");
		return order.getId();
	}

	@Transactional(rollbackFor = Exception.class)
	public void complete(Long id) {
		assertPlatform();
		LocationTransferOrder order = orderMapper.selectByIdForUpdate(id);
		Assert.notNull(order, "库位调整单不存在");
		Assert.isTrue(LocationTransferStatus.PENDING.name().equals(order.getOrderStatus()),
				"只有待调整的库位调整单可以完成");
		List<LocationTransferItem> items = itemService.getByOrderId(id).stream()
				.filter(item -> item.getSourceInventoryId() != null && item.getTargetLocationId() != null)
				.sorted(Comparator.comparing(LocationTransferItem::getSourceInventoryId)
						.thenComparing(LocationTransferItem::getId))
				.collect(Collectors.toList());
		Assert.notEmpty(items, "调整单没有逻辑库位明细");

		Map<Long, WmsLocationInventory> sources = new LinkedHashMap<>();
		for (Long sourceId : items.stream().map(LocationTransferItem::getSourceInventoryId).distinct()
				.sorted().collect(Collectors.toList())) {
			WmsLocationInventory source = inventoryMapper.selectForUpdate(sourceId);
			Assert.notNull(source, "源库存不存在：" + sourceId);
			sources.put(sourceId, source);
		}
		Map<Long, WmsLocation> targets = new LinkedHashMap<>();
		for (Long targetId : items.stream().map(LocationTransferItem::getTargetLocationId).distinct()
				.sorted().collect(Collectors.toList())) {
			WmsLocation target = locationMapper.selectLogicalByIdForUpdate(targetId);
			Assert.notNull(target, "目标库位不存在：" + targetId);
			targets.put(targetId, target);
		}
		Map<Long, WmsZone> zones = zoneMap(order.getWarehouseId());
		Set<String> allowedRacks = rackAssignmentService.activeRackNos(order.getWarehouseId(), order.getWmsTenantId());
		Map<Long, Integer> requestedBySource = new HashMap<>();
		for (LocationTransferItem item : items) {
			WmsLocationInventory source = sources.get(item.getSourceInventoryId());
			WmsLocation target = targets.get(item.getTargetLocationId());
			Assert.isTrue(order.getWarehouseId().equals(source.getWarehouseId()), "源库存不属于调整单仓库");
			Assert.isTrue(order.getErpTenantId().equals(source.getErpTenantId()), "源库存不属于调整单货主");
			Assert.isTrue(order.getWarehouseId().equals(target.getWarehouseId()), "目标库位不属于调整单仓库");
			validateMovement(source.getQuality(), target, zoneType(target, zones.get(target.getZoneId())));
			Assert.isTrue(hasTargetAccess(target, zoneType(target, zones.get(target.getZoneId())), allowedRacks),
					"目标库位不属于当前服务商且不是公共暂存区：" + target.getLocationCode());
			requestedBySource.merge(source.getId(), item.getQuantity(), Integer::sum);
		}
		for (Map.Entry<Long, Integer> request : requestedBySource.entrySet()) {
			validateMovableQuantity(sources.get(request.getKey()), request.getValue());
		}
		validateCompletionCapacity(items, sources, targets);
		for (LocationTransferItem item : items) {
			inventoryService.move(item.getSourceInventoryId(), item.getTargetLocationId(), item.getQuantity(),
					com.erp.admin.wms.model.dto.InventoryMutationContext.builder()
							.eventType(com.erp.admin.wms.model.enums.InventoryEventType.MOVE)
							.sourceType("LOCATION_TRANSFER").sourceId(order.getId())
							.sourceNo(order.getTransferNo()).operatorId(currentUserId())
							.reason(order.getRemark())
							.idempotencyKey("location-transfer:" + order.getId() + ":" + item.getId()).build());
		}
		Assert.isTrue(orderMapper.casComplete(order.getId(), LocationTransferStatus.PENDING.name(),
				LocationTransferStatus.COMPLETED.name(), currentUserId()) == 1,
				"调整单状态已变化，请刷新后重试");
	}

	public static void validateMovableQuantity(WmsLocationInventory inventory, int quantity) {
		Assert.notNull(inventory, "源库存不存在");
		Assert.isTrue(quantity > 0, "移动数量必须大于0");
		int available = value(inventory.getQuantity()) - value(inventory.getReservedQuantity());
		Assert.isTrue(quantity <= available, "可移动数量不足，已预占库存禁止移动");
	}

	public static void validateMovement(String quality, WmsLocation target, String targetZoneType) {
		Assert.notNull(target, "目标库位不存在");
		Assert.isTrue(!DEFECTIVE.equalsIgnoreCase(quality) && !DAMAGED.equalsIgnoreCase(quality),
				"不良品禁止移库，只能通过报废流程处理");
		Assert.isTrue(!DEFECTIVE.equalsIgnoreCase(targetZoneType), "非不良品不能移入不良品区");
	}

	public static void validateTargetAccess(WmsLocation target, Set<String> allowedRacks) {
		Assert.notNull(target, "目标库位不存在");
		boolean publicTemp = Integer.valueOf(1).equals(target.getPublicShared())
				&& "TEMP".equalsIgnoreCase(target.getLocationType());
		Assert.isTrue(publicTemp || (allowedRacks != null && allowedRacks.contains(target.getRackNo())),
				"目标库位不属于当前服务商且不是公共暂存区：" + target.getLocationCode());
	}

	private void validateLines(LogicalLocationTransferCreateDTO dto,
			List<LogicalLocationTransferCreateDTO.Item> lines, Map<Long, WmsLocationInventory> sources,
			Map<Long, WmsLocation> locations, Map<Long, WmsZone> zones, Set<String> allowedRacks) {
		Map<Long, Integer> requestedBySource = new HashMap<>();
		for (LogicalLocationTransferCreateDTO.Item line : lines) {
			WmsLocationInventory source = sources.get(line.getSourceInventoryId());
			Assert.notNull(source, "源库存不存在：" + line.getSourceInventoryId());
			Assert.isTrue(dto.getWarehouseId().equals(source.getWarehouseId()), "源库存不属于所选仓库");
			Assert.isTrue(dto.getErpTenantId().equals(source.getErpTenantId()), "源库存不属于所选货主");
			WmsLocation sourceLocation = locations.get(source.getLocationId());
			WmsLocation target = locations.get(line.getTargetLocationId());
			Assert.notNull(sourceLocation, "源库位不存在");
			Assert.notNull(target, "目标库位不存在：" + line.getTargetLocationId());
			Assert.isTrue(dto.getWarehouseId().equals(target.getWarehouseId()), "目标库位不属于所选仓库");
			Assert.isTrue(!source.getLocationId().equals(target.getId()), "源库位和目标库位不能相同");
			validateMovement(source.getQuality(), target, zoneType(target, zones.get(target.getZoneId())));
			validateTargetAccess(target, allowedRacks);
			Assert.isTrue(hasTargetAccess(sourceLocation,
					zoneType(sourceLocation, zones.get(sourceLocation.getZoneId())), allowedRacks),
					"源库位不属于当前服务商且不是公共暂存区：" + sourceLocation.getLocationCode());
			requestedBySource.merge(source.getId(), line.getQuantity(), Integer::sum);
		}
		for (Map.Entry<Long, Integer> request : requestedBySource.entrySet()) {
			validateMovableQuantity(sources.get(request.getKey()), request.getValue());
		}
	}

	private void validateCapacity(List<LogicalLocationTransferCreateDTO.Item> lines,
			Map<Long, WmsLocationInventory> sources, Map<Long, WmsLocation> locations) {
		Map<Long, List<LogicalLocationTransferCreateDTO.Item>> byTarget = lines.stream()
				.collect(Collectors.groupingBy(LogicalLocationTransferCreateDTO.Item::getTargetLocationId));
		Map<String, Sku> skus = skuMap(sources.values());
		for (Map.Entry<Long, List<LogicalLocationTransferCreateDTO.Item>> entry : byTarget.entrySet()) {
			Assert.notNull(locations.get(entry.getKey()), "目标库位不存在：" + entry.getKey());
			List<LocationCapacityService.PlacementLine> additions = new ArrayList<>();
			for (LogicalLocationTransferCreateDTO.Item line : entry.getValue()) {
				WmsLocationInventory source = sources.get(line.getSourceInventoryId());
				Sku sku = skus.get(source.getSkuCode());
				Assert.notNull(sku, "SKU不存在：" + source.getSkuCode());
				additions.add(LocationCapacityService.fromSku(sku, line.getQuantity()));
			}
			assertCapacity(entry.getKey(), additions, locations.get(entry.getKey()).getLocationCode());
		}
	}

	private void validateCompletionCapacity(List<LocationTransferItem> items,
			Map<Long, WmsLocationInventory> sources,
			Map<Long, WmsLocation> targets) {
		Map<Long, List<LocationTransferItem>> byTarget = items.stream()
				.collect(Collectors.groupingBy(LocationTransferItem::getTargetLocationId));
		Map<String, Sku> skus = skuMap(sources.values());
		for (Map.Entry<Long, List<LocationTransferItem>> entry : byTarget.entrySet()) {
			List<LocationCapacityService.PlacementLine> additions = new ArrayList<>();
			for (LocationTransferItem item : entry.getValue()) {
				WmsLocationInventory source = sources.get(item.getSourceInventoryId());
				Sku sku = skus.get(source.getSkuCode());
				Assert.notNull(sku, "SKU不存在：" + source.getSkuCode());
				additions.add(LocationCapacityService.fromSku(sku, item.getQuantity()));
			}
			assertCapacity(entry.getKey(), additions, targets.get(entry.getKey()).getLocationCode());
		}
	}

	private void assertCapacity(Long locationId, List<LocationCapacityService.PlacementLine> additions,
			String locationCode) {
		LocationCapacityVO capacity = capacityService.evaluate(locationId, additions);
		Assert.isTrue(capacity.isVolumeAllowed(), "目标库位空间不足：" + locationCode);
		Assert.isTrue(capacity.isWeightAllowed(), "目标库位承重不足：" + locationCode);
		Assert.isTrue(capacity.isSkuKindsAllowed(), "目标库位SKU种类数超限：" + locationCode);
	}

	private Map<Long, WmsLocationInventory> sourceMap(List<LogicalLocationTransferCreateDTO.Item> lines) {
		Set<Long> ids = lines.stream().map(LogicalLocationTransferCreateDTO.Item::getSourceInventoryId)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		return inventoryMapper.selectBatchIds(ids).stream()
				.collect(Collectors.toMap(WmsLocationInventory::getId, Function.identity(), (left, right) -> left));
	}

	private Map<String, Sku> skuMap(Collection<WmsLocationInventory> sources) {
		Set<String> codes = sources.stream().map(WmsLocationInventory::getSkuCode).collect(Collectors.toSet());
		if (codes.isEmpty()) {
			return Collections.emptyMap();
		}
		return skuMapper.selectBySkuCodes(codes).stream()
				.collect(Collectors.toMap(Sku::getSkuCode, Function.identity(), (left, right) -> left));
	}

	private List<LogicalLocationTransferCreateDTO.Item> mergeLines(
			List<LogicalLocationTransferCreateDTO.Item> source) {
		Map<String, LogicalLocationTransferCreateDTO.Item> merged = new LinkedHashMap<>();
		for (LogicalLocationTransferCreateDTO.Item line : source) {
			Assert.notNull(line.getSourceInventoryId(), "源库存不能为空");
			Assert.notNull(line.getTargetLocationId(), "目标库位不能为空");
			Assert.isTrue(line.getQuantity() != null && line.getQuantity() > 0, "移动数量必须大于0");
			String key = line.getSourceInventoryId() + "|" + line.getTargetLocationId();
			LogicalLocationTransferCreateDTO.Item target = merged.get(key);
			if (target == null) {
				target = new LogicalLocationTransferCreateDTO.Item();
				target.setSourceInventoryId(line.getSourceInventoryId());
				target.setTargetLocationId(line.getTargetLocationId());
				target.setQuantity(line.getQuantity());
				target.setRemark(line.getRemark());
				merged.put(key, target);
			}
			else {
				target.setQuantity(target.getQuantity() + line.getQuantity());
			}
		}
		return new ArrayList<>(merged.values());
	}

	private Map<Long, WmsLocation> locationMap(Long warehouseId) {
		return locationService.listByWarehouse(warehouseId).stream()
				.collect(Collectors.toMap(WmsLocation::getId, Function.identity(), (left, right) -> left));
	}

	private Map<Long, WmsZone> zoneMap(Long warehouseId) {
		return zoneService.listByWarehouse(warehouseId).stream()
				.collect(Collectors.toMap(WmsZone::getId, Function.identity(), (left, right) -> left));
	}

	private String zoneType(WmsLocation location, WmsZone zone) {
		return zone != null && StringUtils.hasText(zone.getZoneType()) ? zone.getZoneType()
				: location.getLocationType();
	}

	private static boolean hasTargetAccess(WmsLocation target, String zoneType, Set<String> allowedRacks) {
		return (Integer.valueOf(1).equals(target.getPublicShared()) && "TEMP".equalsIgnoreCase(zoneType))
				|| (allowedRacks != null && allowedRacks.contains(target.getRackNo()));
	}

	private SysTenant requireOwner(Long erpTenantId) {
		Assert.notNull(erpTenantId, "货主不能为空");
		SysTenant owner = tenantMapper.selectById(erpTenantId);
		Assert.notNull(owner, "货主不存在");
		Assert.notNull(owner.getParentWmsTenantId(), "货主未绑定WMS服务商");
		return owner;
	}

	private void saveOrderWithRetry(LocationTransferOrder order) {
		for (int attempt = 0; attempt < 3; attempt++) {
			order.setTransferNo(generateTransferNo());
			try {
				Assert.isTrue(orderMapper.insert(order) == 1, "库位调整单保存失败");
				return;
			}
			catch (DuplicateKeyException ex) {
				order.setId(null);
				if (attempt == 2) {
					throw ex;
				}
			}
		}
	}

	private String generateTransferNo() {
		String prefix = "LT" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		return prefix + String.format("%04d", orderMapper.countTodayOrders(prefix) + 1);
	}

	private void assertPlatform() {
		String type = tenantIdentityService.currentIdentity(null).getIdentityType();
		if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(type)) {
			throw new BusinessException(403, "仅海外仓平台可操作库位调整单");
		}
	}

	private Long currentUserId() {
		try {
			return principalAccessor.getUserId();
		}
		catch (Exception ignore) {
			return null;
		}
	}

	private static int value(Integer number) {
		return number == null ? 0 : number;
	}

}
