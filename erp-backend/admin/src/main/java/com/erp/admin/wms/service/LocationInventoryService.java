package com.erp.admin.wms.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.erp.admin.wms.mapper.WmsInventoryReservationMapper;
import com.erp.admin.wms.mapper.WmsLocationInventoryMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.dto.InventoryReservationRequest;
import com.erp.admin.wms.model.dto.InventoryMutationContext;
import com.erp.admin.wms.model.dto.InventoryMutationLine;
import com.erp.admin.wms.model.dto.LocationInventoryKey;
import com.erp.admin.wms.model.enums.InventoryEventType;
import com.erp.admin.wms.model.enums.LocationInventoryQuality;
import com.erp.admin.wms.model.entity.WmsInventoryReservation;
import com.erp.admin.wms.model.entity.WmsLocationInventory;
import com.erp.admin.wms.model.entity.WmsLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class LocationInventoryService {

	private static final String RESERVED = "RESERVED";

	private final WmsLocationInventoryMapper inventoryMapper;

	private final WmsInventoryReservationMapper reservationMapper;

	private final WmsLocationMapper locationMapper;

	private final StocktakeFreezeService stocktakeFreezeService;

	private final InventoryEventService inventoryEventService;

	public List<String> occupiedLocationCodes(Long warehouseId) {
		Assert.notNull(warehouseId, "仓库不能为空");
		return inventoryMapper.listOccupiedLocationCodes(warehouseId);
	}

	@Transactional(rollbackFor = Exception.class)
	public void increase(LocationInventoryKey key, int quantity) {
		increase(key, quantity, fallbackContext(InventoryEventType.INBOUND_PUTAWAY));
	}

	@Transactional(rollbackFor = Exception.class)
	public void increase(LocationInventoryKey key, int quantity, InventoryMutationContext context) {
		validateKey(key);
		positive(quantity);
		key.setQuality(LocationInventoryQuality.normalize(key.getQuality()));
		if (alreadyHandled(key.getTenantId(), context)) {
			return;
		}
		assertLocationMutable(key.getWarehouseId(), key.getLocationId());
		WmsLocationInventory existing = inventoryMapper.selectByKeyForUpdate(key);
		if (existing == null) {
			WmsLocationInventory created = inventory(key, quantity);
			try {
				Assert.isTrue(inventoryMapper.insert(created) == 1, "库存增加失败，请重试");
				append(context, created, Collections.singletonList(line(created, quantity, 0, quantity, 0, 0, null)));
				return;
			}
			catch (DuplicateKeyException ex) {
				existing = inventoryMapper.selectByKeyForUpdate(key);
				Assert.notNull(existing, "库存并发创建失败，请重试");
			}
		}
		Assert.isTrue(inventoryMapper.increaseQuantity(existing.getId(), quantity, value(existing.getVersion())) == 1,
				"库存已变化，请刷新后重试");
		append(context, existing, Collections.singletonList(line(existing, quantity, value(existing.getQuantity()),
				value(existing.getQuantity()) + quantity, value(existing.getReservedQuantity()), 0, null)));
	}

	@Transactional(rollbackFor = Exception.class)
	public void decrease(LocationInventoryKey key, int quantity) {
		decrease(key, quantity, fallbackContext(InventoryEventType.DECREASE));
	}

	@Transactional(rollbackFor = Exception.class)
	public void decrease(LocationInventoryKey key, int quantity, InventoryMutationContext context) {
		validateKey(key);
		positive(quantity);
		key.setQuality(LocationInventoryQuality.normalize(key.getQuality()));
		if (alreadyHandled(key.getTenantId(), context)) {
			return;
		}
		assertLocationMutable(key.getWarehouseId(), key.getLocationId());
		WmsLocationInventory existing = inventoryMapper.selectByKeyForUpdate(key);
		Assert.notNull(existing, "库存不存在");
		Assert.isTrue(inventoryMapper.decreaseAvailableQuantity(existing.getId(), quantity,
				value(existing.getVersion())) == 1, "可用库存不足或库存已变化");
		append(context, existing, Collections.singletonList(line(existing, -quantity, value(existing.getQuantity()),
				value(existing.getQuantity()) - quantity, value(existing.getReservedQuantity()), 0, null)));
	}

	@Transactional(rollbackFor = Exception.class)
	public List<Long> reserve(InventoryReservationRequest request) {
		return reserve(request, fallbackContext(InventoryEventType.RESERVE));
	}

	@Transactional(rollbackFor = Exception.class)
	public List<Long> reserve(InventoryReservationRequest request, InventoryMutationContext context) {
		validateRequest(request);
		request.setQuality(LocationInventoryQuality.normalize(request.getQuality()));
		if (alreadyHandled(request.getTenantId(), context)) {
			return Collections.emptyList();
		}
		List<WmsLocationInventory> candidates = inventoryMapper.selectOwnedAvailableForUpdate(request.getTenantId(),
				request.getWmsTenantId(), request.getWarehouseId(), request.getErpTenantId(), request.getSkuCode(),
				request.getQuality());
		if (candidates == null) {
			candidates = Collections.emptyList();
		}
		int remaining = request.getQuantity();
		List<Long> inventoryIds = new ArrayList<>();
		List<InventoryMutationLine> eventLines = new ArrayList<>();
		for (WmsLocationInventory candidate : candidates) {
			if (remaining == 0) {
				break;
			}
			int available = value(candidate.getQuantity()) - value(candidate.getReservedQuantity());
			int allocated = Math.min(remaining, Math.max(0, available));
			if (allocated == 0) {
				continue;
			}
			assertLocationMutable(candidate.getWarehouseId(), candidate.getLocationId());
			Assert.isTrue(inventoryMapper.reserveQuantity(candidate.getId(), allocated,
					value(candidate.getVersion())) == 1, "库存预占冲突，请刷新后重试");
			WmsInventoryReservation reservation = new WmsInventoryReservation();
			reservation.setFulfillmentOrderId(request.getFulfillmentOrderId());
			reservation.setFulfillmentItemId(request.getFulfillmentItemId());
			reservation.setInventoryId(candidate.getId());
			reservation.setLocationId(candidate.getLocationId());
			reservation.setQuantity(allocated);
			reservation.setReservationStatus(RESERVED);
			reservation.setVersion(0);
			Assert.isTrue(reservationMapper.insert(reservation) == 1, "库存预占明细保存失败");
			inventoryIds.add(candidate.getId());
			eventLines.add(line(candidate, 0, value(candidate.getQuantity()), value(candidate.getQuantity()),
					value(candidate.getReservedQuantity()), allocated, null));
			remaining -= allocated;
		}
		Assert.isTrue(remaining == 0, "库存不足，缺少 " + remaining + " 件");
		inventoryEventService.append(context, request.getTenantId(), request.getWmsTenantId(),
				request.getErpTenantId(), request.getWarehouseId(), eventLines);
		return inventoryIds;
	}

	@Transactional(rollbackFor = Exception.class)
	public void release(Long fulfillmentOrderId) {
		release(fulfillmentOrderId, fallbackContext(InventoryEventType.RELEASE));
	}

	@Transactional(rollbackFor = Exception.class)
	public void release(Long fulfillmentOrderId, InventoryMutationContext context) {
		changeReservations(fulfillmentOrderId, false, context);
	}

	@Transactional(rollbackFor = Exception.class)
	public void ship(Long fulfillmentOrderId) {
		ship(fulfillmentOrderId, fallbackContext(InventoryEventType.SHIP));
	}

	@Transactional(rollbackFor = Exception.class)
	public void ship(Long fulfillmentOrderId, InventoryMutationContext context) {
		changeReservations(fulfillmentOrderId, true, context);
	}

	@Transactional(rollbackFor = Exception.class)
	public void move(Long sourceInventoryId, Long targetLocationId, int quantity) {
		move(sourceInventoryId, targetLocationId, quantity, fallbackContext(InventoryEventType.MOVE));
	}

	@Transactional(rollbackFor = Exception.class)
	public void move(Long sourceInventoryId, Long targetLocationId, int quantity, InventoryMutationContext context) {
		Assert.notNull(sourceInventoryId, "源库存不能为空");
		Assert.notNull(targetLocationId, "目标库位不能为空");
		positive(quantity);
		WmsLocationInventory source = inventoryMapper.selectForUpdate(sourceInventoryId);
		Assert.notNull(source, "源库存不存在");
		if (alreadyHandled(source.getTenantId(), context)) {
			return;
		}
		Assert.isTrue(!targetLocationId.equals(source.getLocationId()), "源库位和目标库位不能相同");
		assertLocationMutable(source.getWarehouseId(), source.getLocationId());
		assertLocationMutable(source.getWarehouseId(), targetLocationId);
		int available = value(source.getQuantity()) - value(source.getReservedQuantity());
		Assert.isTrue(quantity <= available, "可移动数量不足，已预占库存禁止移动");

		LocationInventoryKey targetKey = keyOf(source, targetLocationId);
		WmsLocationInventory target = inventoryMapper.selectByKeyForUpdate(targetKey);
		boolean createdTarget = target == null;
		Assert.isTrue(inventoryMapper.decreaseAvailableQuantity(source.getId(), quantity,
				value(source.getVersion())) == 1, "源库存已变化，请重试");
		if (target == null) {
			target = inventory(targetKey, quantity);
			Assert.isTrue(inventoryMapper.insert(target) == 1, "目标库存创建失败");
		}
		else {
			Assert.isTrue(inventoryMapper.increaseQuantity(target.getId(), quantity,
					value(target.getVersion())) == 1, "目标库存已变化，请重试");
		}
		List<InventoryMutationLine> lines = new ArrayList<>();
		lines.add(line(source, -quantity, value(source.getQuantity()), value(source.getQuantity()) - quantity,
				value(source.getReservedQuantity()), 0, targetLocationId));
		int targetBefore = createdTarget ? 0 : value(target.getQuantity());
		lines.add(line(target, quantity, targetBefore, targetBefore + quantity,
				value(target.getReservedQuantity()), 0, source.getLocationId()));
		append(context, source, lines);
	}

	@Transactional(rollbackFor = Exception.class)
	public WmsLocationInventory adjustCountedQuantity(Long inventoryId, int countedQuantity) {
		return adjustCountedQuantity(inventoryId, countedQuantity, null);
	}

	@Transactional(rollbackFor = Exception.class)
	public WmsLocationInventory adjustCountedQuantity(Long inventoryId, int countedQuantity, InventoryMutationContext context) {
		Assert.notNull(inventoryId, "盘点库存不能为空");
		Assert.isTrue(countedQuantity >= 0, "实盘数量不能为负数");
		WmsLocationInventory inventory = inventoryMapper.selectForUpdate(inventoryId);
		Assert.notNull(inventory, "盘点库存不存在");
		validateCountedQuantity(countedQuantity, value(inventory.getReservedQuantity()));
		int delta = countedQuantity - value(inventory.getQuantity());
		InventoryMutationContext effectiveContext = context == null
				? fallbackContext(delta >= 0 ? InventoryEventType.STOCKTAKE_GAIN : InventoryEventType.STOCKTAKE_LOSS)
				: context;
		if (alreadyHandled(inventory.getTenantId(), effectiveContext)) {
			return inventory;
		}
		if (delta > 0) {
			Assert.isTrue(inventoryMapper.increaseQuantity(inventoryId, delta, value(inventory.getVersion())) == 1,
					"盘点期间库存已变化，请取消后重新盘点");
		}
		else if (delta < 0) {
			Assert.isTrue(inventoryMapper.decreaseAvailableQuantity(inventoryId, -delta,
					value(inventory.getVersion())) == 1, "盘点期间库存已变化，请取消后重新盘点");
		}
		inventory.setQuantity(countedQuantity);
		if (delta != 0) {
			append(effectiveContext, inventory, Collections.singletonList(line(inventory, delta,
					countedQuantity - delta, countedQuantity, value(inventory.getReservedQuantity()), 0, null)));
		}
		return inventory;
	}

	@Transactional(rollbackFor = Exception.class)
	public WmsLocationInventory reserveInventory(Long inventoryId, int quantity) {
		return reserveInventory(inventoryId, quantity, fallbackContext(InventoryEventType.SCRAP_RESERVE));
	}

	@Transactional(rollbackFor = Exception.class)
	public WmsLocationInventory reserveInventory(Long inventoryId, int quantity, InventoryMutationContext context) {
		Assert.notNull(inventoryId, "库存不能为空");
		positive(quantity);
		WmsLocationInventory inventory = inventoryMapper.selectForUpdate(inventoryId);
		Assert.notNull(inventory, "库存不存在");
		if (alreadyHandled(inventory.getTenantId(), context)) {
			return inventory;
		}
		assertLocationMutable(inventory.getWarehouseId(), inventory.getLocationId());
		Assert.isTrue(inventoryMapper.reserveQuantity(inventoryId, quantity, value(inventory.getVersion())) == 1,
				"可报废数量不足或库存已变化");
		append(context, inventory, Collections.singletonList(line(inventory, 0, value(inventory.getQuantity()),
				value(inventory.getQuantity()), value(inventory.getReservedQuantity()), quantity, null)));
		return inventory;
	}

	@Transactional(rollbackFor = Exception.class)
	public void releaseInventory(Long inventoryId, int quantity) {
		releaseInventory(inventoryId, quantity, fallbackContext(InventoryEventType.SCRAP_RELEASE));
	}

	@Transactional(rollbackFor = Exception.class)
	public void releaseInventory(Long inventoryId, int quantity, InventoryMutationContext context) {
		Assert.notNull(inventoryId, "库存不能为空");
		positive(quantity);
		WmsLocationInventory inventory = inventoryMapper.selectForUpdate(inventoryId);
		Assert.notNull(inventory, "库存不存在");
		if (alreadyHandled(inventory.getTenantId(), context)) {
			return;
		}
		assertLocationMutable(inventory.getWarehouseId(), inventory.getLocationId());
		Assert.isTrue(inventoryMapper.releaseQuantity(inventoryId, quantity, value(inventory.getVersion())) == 1,
				"报废预留已变化，请刷新后重试");
		append(context, inventory, Collections.singletonList(line(inventory, 0, value(inventory.getQuantity()),
				value(inventory.getQuantity()), value(inventory.getReservedQuantity()), -quantity, null)));
	}

	@Transactional(rollbackFor = Exception.class)
	public void scrapReservedInventory(Long inventoryId, int quantity) {
		scrapReservedInventory(inventoryId, quantity, fallbackContext(InventoryEventType.SCRAP));
	}

	@Transactional(rollbackFor = Exception.class)
	public void scrapReservedInventory(Long inventoryId, int quantity, InventoryMutationContext context) {
		Assert.notNull(inventoryId, "库存不能为空");
		positive(quantity);
		WmsLocationInventory inventory = inventoryMapper.selectForUpdate(inventoryId);
		Assert.notNull(inventory, "库存不存在");
		if (alreadyHandled(inventory.getTenantId(), context)) {
			return;
		}
		assertLocationMutable(inventory.getWarehouseId(), inventory.getLocationId());
		Assert.isTrue(inventoryMapper.shipQuantity(inventoryId, quantity, value(inventory.getVersion())) == 1,
				"待报废库存不足或库存已变化");
		append(context, inventory, Collections.singletonList(line(inventory, -quantity, value(inventory.getQuantity()),
				value(inventory.getQuantity()) - quantity, value(inventory.getReservedQuantity()), -quantity, null)));
	}

	public static void validateCountedQuantity(int countedQuantity, int reservedQuantity) {
		Assert.isTrue(countedQuantity >= reservedQuantity, "实盘数量不能小于已预占数量");
	}

	private void changeReservations(Long fulfillmentOrderId, boolean shipping, InventoryMutationContext context) {
		Assert.notNull(fulfillmentOrderId, "履约单不能为空");
		List<WmsInventoryReservation> reservations = reservationMapper.selectReservedForUpdate(fulfillmentOrderId);
		Assert.notEmpty(reservations, "履约单没有可处理的库存预占");
		List<InventoryMutationLine> eventLines = new ArrayList<>();
		WmsLocationInventory headerInventory = null;
		for (WmsInventoryReservation reservation : reservations) {
			WmsLocationInventory inventory = inventoryMapper.selectForUpdate(reservation.getInventoryId());
			Assert.notNull(inventory, "预占对应库存不存在：" + reservation.getInventoryId());
			if (headerInventory == null) {
				headerInventory = inventory;
				if (alreadyHandled(inventory.getTenantId(), context)) {
					return;
				}
			}
			assertLocationMutable(inventory.getWarehouseId(), inventory.getLocationId());
			int updated = shipping
					? inventoryMapper.shipQuantity(inventory.getId(), reservation.getQuantity(), value(inventory.getVersion()))
					: inventoryMapper.releaseQuantity(inventory.getId(), reservation.getQuantity(), value(inventory.getVersion()));
			Assert.isTrue(updated == 1, shipping ? "库存签出冲突，请刷新后重试" : "库存释放冲突，请刷新后重试");
			Assert.isTrue(reservationMapper.markStatus(reservation.getId(), RESERVED,
					shipping ? "SHIPPED" : "RELEASED", value(reservation.getVersion())) == 1,
					"预占状态已变化，请刷新后重试");
			eventLines.add(line(inventory, shipping ? -reservation.getQuantity() : 0,
					value(inventory.getQuantity()), shipping ? value(inventory.getQuantity()) - reservation.getQuantity()
							: value(inventory.getQuantity()), value(inventory.getReservedQuantity()),
					-reservation.getQuantity(), null));
		}
		append(context, headerInventory, eventLines);
	}

	private void assertLocationMutable(Long warehouseId, Long locationId) {
		WmsLocation location = locationMapper.selectById(locationId);
		Assert.notNull(location, "库存库位不存在：" + locationId);
		Assert.isTrue(warehouseId.equals(location.getWarehouseId()), "库存库位不属于当前仓库");
		stocktakeFreezeService.assertLocationMutable(warehouseId, location.getLocationCode());
	}

	private WmsLocationInventory inventory(LocationInventoryKey key, int quantity) {
		WmsLocationInventory row = new WmsLocationInventory();
		row.setTenantId(key.getTenantId());
		row.setWmsTenantId(key.getWmsTenantId());
		row.setErpTenantId(key.getErpTenantId());
		row.setWarehouseId(key.getWarehouseId());
		row.setLocationId(key.getLocationId());
		row.setSkuCode(key.getSkuCode());
		row.setQuality(key.getQuality());
		row.setQuantity(quantity);
		row.setReservedQuantity(0);
		row.setVersion(0);
		row.setDeleted(0L);
		return row;
	}

	private LocationInventoryKey keyOf(WmsLocationInventory row, Long locationId) {
		LocationInventoryKey key = new LocationInventoryKey();
		key.setTenantId(row.getTenantId());
		key.setWmsTenantId(row.getWmsTenantId());
		key.setErpTenantId(row.getErpTenantId());
		key.setWarehouseId(row.getWarehouseId());
		key.setLocationId(locationId);
		key.setSkuCode(row.getSkuCode());
		key.setQuality(row.getQuality());
		return key;
	}

	private boolean alreadyHandled(Long tenantId, InventoryMutationContext context) {
		Assert.notNull(context, "库存事件上下文不能为空");
		return inventoryEventService.exists(tenantId, context.getIdempotencyKey());
	}

	private void append(InventoryMutationContext context, WmsLocationInventory header,
			List<InventoryMutationLine> lines) {
		Assert.notNull(header, "库存事件归属不能为空");
		inventoryEventService.append(context, header.getTenantId(), header.getWmsTenantId(),
				header.getErpTenantId(), header.getWarehouseId(), lines);
	}

	private InventoryMutationLine line(WmsLocationInventory inventory, int quantityDelta, int beforeQuantity,
			int afterQuantity, int beforeReserved, int reservedDelta, Long counterpartLocationId) {
		return InventoryMutationLine.builder()
				.inventoryId(inventory.getId())
				.locationId(inventory.getLocationId())
				.counterpartLocationId(counterpartLocationId)
				.skuCode(inventory.getSkuCode())
				.quality(LocationInventoryQuality.normalize(inventory.getQuality()))
				.quantityDelta(quantityDelta)
				.reservedDelta(reservedDelta)
				.beforeQuantity(beforeQuantity)
				.afterQuantity(afterQuantity)
				.beforeReserved(beforeReserved)
				.afterReserved(beforeReserved + reservedDelta)
				.build();
	}

	private InventoryMutationContext fallbackContext(InventoryEventType eventType) {
		return InventoryMutationContext.builder()
				.eventType(eventType)
				.sourceType("SYSTEM")
				.reason("业务库存变更")
				.idempotencyKey("legacy:" + eventType.name() + ":" + UUID.randomUUID())
				.build();
	}

	private void validateRequest(InventoryReservationRequest request) {
		Assert.notNull(request, "预占请求不能为空");
		Assert.notNull(request.getFulfillmentOrderId(), "履约单不能为空");
		Assert.notNull(request.getFulfillmentItemId(), "履约商品不能为空");
		Assert.notNull(request.getTenantId(), "平台租户不能为空");
		Assert.notNull(request.getWmsTenantId(), "服务商不能为空");
		Assert.notNull(request.getErpTenantId(), "货主不能为空");
		Assert.notNull(request.getWarehouseId(), "仓库不能为空");
		Assert.hasText(request.getSkuCode(), "SKU不能为空");
		Assert.hasText(request.getQuality(), "品质不能为空");
		Assert.notNull(request.getQuantity(), "数量不能为空");
		positive(request.getQuantity());
	}

	private void validateKey(LocationInventoryKey key) {
		Assert.notNull(key, "库存维度不能为空");
		Assert.notNull(key.getTenantId(), "平台租户不能为空");
		Assert.notNull(key.getWmsTenantId(), "服务商不能为空");
		Assert.notNull(key.getErpTenantId(), "货主不能为空");
		Assert.notNull(key.getWarehouseId(), "仓库不能为空");
		Assert.notNull(key.getLocationId(), "库位不能为空");
		Assert.hasText(key.getSkuCode(), "SKU不能为空");
		Assert.hasText(key.getQuality(), "品质不能为空");
	}

	private void positive(int quantity) {
		Assert.isTrue(quantity > 0, "数量必须大于0");
	}

	private int value(Integer value) {
		return value == null ? 0 : value;
	}

}
