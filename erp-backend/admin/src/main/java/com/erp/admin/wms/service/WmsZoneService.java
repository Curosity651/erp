package com.erp.admin.wms.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.erp.admin.wms.enums.WmsResultCode;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.mapper.WmsZoneMapper;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.entity.WmsZone;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 品质分区服务（C1）。
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class WmsZoneService extends ExtendServiceImpl<WmsZoneMapper, WmsZone> {

	/** 默认分区定义：类型、名称、是否可分配。 */
	private static final String[][] DEFAULT_ZONES = {
			// STANDARD/RETURN 可分配；DEFECTIVE/TEMP 不可分配
			{ "STANDARD", "标准区", "1" }, { "DEFECTIVE", "不良品区", "0" }, { "RETURN", "退货区", "1" },
			{ "TEMP", "暂存区", "0" } };

	private final WmsLocationMapper wmsLocationMapper;

	private final WmsPhysicalInventoryMapper wmsPhysicalInventoryMapper;

	private final WmsInventoryAggregator wmsInventoryAggregator;

	public List<WmsZone> listByWarehouse(Long warehouseId) {
		return baseMapper.listByWarehouse(warehouseId);
	}

	/**
	 * 把选中库位改到目标分区（联动口径）：更新库位 zone_id；同步该批库位下现有批次的 zone_id +
	 * allocatable（取目标分区可分配性），并重算受影响聚合键的可用库存快照。整体事务。
	 * @param warehouseId 仓库ID
	 * @param locationIds 库位ID集合
	 * @param zoneId 目标分区ID
	 * @return 变更的库位数
	 */
	@Transactional(rollbackFor = Exception.class)
	public int moveLocationsToZone(Long warehouseId, List<Long> locationIds, Long zoneId) {
		Assert.notEmpty(locationIds, "请选择库位");
		WmsZone zone = baseMapper.selectById(zoneId);
		if (zone == null || !warehouseId.equals(zone.getWarehouseId())) {
			throw new BusinessException(400, "分区不存在或不属于该仓库");
		}

		// 0. 目标库位（限本仓）与其编码
		List<WmsLocation> targets = new ArrayList<>();
		List<String> codes = new ArrayList<>();
		for (WmsLocation loc : wmsLocationMapper.selectBatchIds(locationIds)) {
			if (warehouseId.equals(loc.getWarehouseId())) {
				targets.add(loc);
				codes.add(loc.getLocationCode());
			}
		}
		if (codes.isEmpty()) {
			return 0;
		}

		// 守卫：有货物占用(quantity>0)的库位禁止改分区（防破坏品质↔分区不变式；仅锁有货格子，空位放行）
		List<WmsPhysicalInventory> batches = wmsPhysicalInventoryMapper.listByWarehouseAndLocationCodes(warehouseId, codes);
		List<String> occupiedCodes = batches.stream()
				.filter(b -> b.getQuantity() != null && b.getQuantity() > 0)
				.map(WmsPhysicalInventory::getLocationCode)
				.distinct()
				.collect(java.util.stream.Collectors.toList());
		if (!occupiedCodes.isEmpty()) {
			throw new BusinessException(WmsResultCode.LOCATION_ZONE_MOVE_OCCUPIED.getCode(),
					WmsResultCode.LOCATION_ZONE_MOVE_OCCUPIED.getMessage() + "：" + String.join("、", occupiedCodes));
		}

		// 1. 更新库位的分区
		for (WmsLocation loc : targets) {
			loc.setZoneId(zoneId);
			wmsLocationMapper.updateById(loc);
		}

		// 2. 联动现有批次（此时均为 quantity=0 的残留批次）：zone_id + allocatable 同步为目标分区口径，收集受影响聚合键
		Map<String, WmsPhysicalInventory> affectedKeys = new LinkedHashMap<>();
		for (WmsPhysicalInventory b : batches) {
			b.setZoneId(zoneId);
			b.setAllocatable(zone.getAllocatable());
			wmsPhysicalInventoryMapper.updateById(b);
			String key = b.getWmsTenantId() + "|" + b.getErpTenantId() + "|" + b.getSkuCode();
			affectedKeys.putIfAbsent(key, b);
		}

		// 3. 重算受影响的 (服务商×货主×仓×SKU) 可用库存快照
		for (WmsPhysicalInventory b : affectedKeys.values()) {
			wmsInventoryAggregator.refreshSnapshot(b.getWmsTenantId(), b.getErpTenantId(), warehouseId, b.getSkuCode());
		}

		return codes.size();
	}

	public Long findDefaultStandardZoneId(Long warehouseId) {
		return baseMapper.findDefaultStandardZoneId(warehouseId);
	}

	/**
	 * 为仓库初始化四类默认分区（已存在则跳过，不重复创建）。
	 * @param warehouseId 仓库ID
	 * @return 新建的分区数
	 */
	@Transactional(rollbackFor = Exception.class)
	public int initDefaultZones(Long warehouseId) {
		if (baseMapper.existsByWarehouse(warehouseId)) {
			return 0;
		}
		List<WmsZone> zones = new ArrayList<>();
		for (String[] def : DEFAULT_ZONES) {
			WmsZone zone = new WmsZone();
			zone.setWarehouseId(warehouseId);
			zone.setZoneType(def[0]);
			zone.setZoneName(def[1]);
			zone.setAllocatable(Integer.parseInt(def[2]));
			zones.add(zone);
		}
		this.saveBatch(zones);
		return zones.size();
	}

}
