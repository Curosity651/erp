package com.erp.admin.wms.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.erp.admin.wms.mapper.WmsInventoryReservationMapper;
import com.erp.admin.wms.mapper.WmsLocationInventoryMapper;
import com.erp.admin.wms.model.dto.InventoryReservationRequest;
import com.erp.admin.wms.model.dto.LocationInventoryKey;
import com.erp.admin.wms.model.entity.WmsInventoryReservation;
import com.erp.admin.wms.model.entity.WmsLocationInventory;
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

	@Transactional(rollbackFor = Exception.class)
	public void increase(LocationInventoryKey key, int quantity) {
		validateKey(key);
		positive(quantity);
		WmsLocationInventory existing = inventoryMapper.selectByKeyForUpdate(key);
		if (existing == null) {
			WmsLocationInventory created = inventory(key, quantity);
			try {
				Assert.isTrue(inventoryMapper.insert(created) == 1, "库存增加失败，请重试");
				return;
			}
			catch (DuplicateKeyException ex) {
				existing = inventoryMapper.selectByKeyForUpdate(key);
				Assert.notNull(existing, "库存并发创建失败，请重试");
			}
		}
		Assert.isTrue(inventoryMapper.increaseQuantity(existing.getId(), quantity, value(existing.getVersion())) == 1,
				"库存已变化，请刷新后重试");
	}

	@Transactional(rollbackFor = Exception.class)
	public void decrease(LocationInventoryKey key, int quantity) {
		validateKey(key);
		positive(quantity);
		WmsLocationInventory existing = inventoryMapper.selectByKeyForUpdate(key);
		Assert.notNull(existing, "库存不存在");
		Assert.isTrue(inventoryMapper.decreaseAvailableQuantity(existing.getId(), quantity,
				value(existing.getVersion())) == 1, "可用库存不足或库存已变化");
	}

	@Transactional(rollbackFor = Exception.class)
	public List<Long> reserve(InventoryReservationRequest request) {
		validateRequest(request);
		List<WmsLocationInventory> candidates = inventoryMapper.selectOwnedAvailableForUpdate(request.getTenantId(),
				request.getWmsTenantId(), request.getWarehouseId(), request.getErpTenantId(), request.getSkuCode(),
				request.getQuality());
		if (candidates == null) {
			candidates = Collections.emptyList();
		}
		int remaining = request.getQuantity();
		List<Long> inventoryIds = new ArrayList<>();
		for (WmsLocationInventory candidate : candidates) {
			if (remaining == 0) {
				break;
			}
			int available = value(candidate.getQuantity()) - value(candidate.getReservedQuantity());
			int allocated = Math.min(remaining, Math.max(0, available));
			if (allocated == 0) {
				continue;
			}
			Assert.isTrue(inventoryMapper.reserveQuantity(candidate.getId(), allocated,
					value(candidate.getVersion())) == 1, "库存预占冲突，请刷新后重试");
			WmsInventoryReservation reservation = new WmsInventoryReservation();
			reservation.setFulfillmentOrderId(request.getFulfillmentOrderId());
			reservation.setInventoryId(candidate.getId());
			reservation.setQuantity(allocated);
			reservation.setReservationStatus(RESERVED);
			reservation.setVersion(0);
			Assert.isTrue(reservationMapper.insert(reservation) == 1, "库存预占明细保存失败");
			inventoryIds.add(candidate.getId());
			remaining -= allocated;
		}
		Assert.isTrue(remaining == 0, "库存不足，缺少 " + remaining + " 件");
		return inventoryIds;
	}

	@Transactional(rollbackFor = Exception.class)
	public void release(Long fulfillmentOrderId) {
		changeReservations(fulfillmentOrderId, false);
	}

	@Transactional(rollbackFor = Exception.class)
	public void ship(Long fulfillmentOrderId) {
		changeReservations(fulfillmentOrderId, true);
	}

	@Transactional(rollbackFor = Exception.class)
	public void move(Long sourceInventoryId, Long targetLocationId, int quantity) {
		Assert.notNull(sourceInventoryId, "源库存不能为空");
		Assert.notNull(targetLocationId, "目标库位不能为空");
		positive(quantity);
		WmsLocationInventory source = inventoryMapper.selectForUpdate(sourceInventoryId);
		Assert.notNull(source, "源库存不存在");
		Assert.isTrue(!targetLocationId.equals(source.getLocationId()), "源库位和目标库位不能相同");
		int available = value(source.getQuantity()) - value(source.getReservedQuantity());
		Assert.isTrue(quantity <= available, "可移动数量不足，已预占库存禁止移动");

		LocationInventoryKey targetKey = keyOf(source, targetLocationId);
		WmsLocationInventory target = inventoryMapper.selectByKeyForUpdate(targetKey);
		Assert.isTrue(inventoryMapper.decreaseAvailableQuantity(source.getId(), quantity,
				value(source.getVersion())) == 1, "源库存已变化，请重试");
		if (target == null) {
			Assert.isTrue(inventoryMapper.insert(inventory(targetKey, quantity)) == 1, "目标库存创建失败");
		}
		else {
			Assert.isTrue(inventoryMapper.increaseQuantity(target.getId(), quantity,
					value(target.getVersion())) == 1, "目标库存已变化，请重试");
		}
	}

	private void changeReservations(Long fulfillmentOrderId, boolean shipping) {
		Assert.notNull(fulfillmentOrderId, "履约单不能为空");
		List<WmsInventoryReservation> reservations = reservationMapper.selectReservedForUpdate(fulfillmentOrderId);
		Assert.notEmpty(reservations, "履约单没有可处理的库存预占");
		for (WmsInventoryReservation reservation : reservations) {
			WmsLocationInventory inventory = inventoryMapper.selectForUpdate(reservation.getInventoryId());
			Assert.notNull(inventory, "预占对应库存不存在：" + reservation.getInventoryId());
			int updated = shipping
					? inventoryMapper.shipQuantity(inventory.getId(), reservation.getQuantity(), value(inventory.getVersion()))
					: inventoryMapper.releaseQuantity(inventory.getId(), reservation.getQuantity(), value(inventory.getVersion()));
			Assert.isTrue(updated == 1, shipping ? "库存签出冲突，请刷新后重试" : "库存释放冲突，请刷新后重试");
			Assert.isTrue(reservationMapper.markStatus(reservation.getId(), RESERVED,
					shipping ? "SHIPPED" : "RELEASED", value(reservation.getVersion())) == 1,
					"预占状态已变化，请刷新后重试");
		}
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

	private void validateRequest(InventoryReservationRequest request) {
		Assert.notNull(request, "预占请求不能为空");
		Assert.notNull(request.getFulfillmentOrderId(), "履约单不能为空");
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
