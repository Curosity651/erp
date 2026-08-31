package com.erp.admin.wms.service;

import java.util.List;
import java.util.stream.Collectors;

import com.erp.admin.wms.enums.WmsResultCode;
import com.erp.admin.wms.mapper.WmsLocationInventoryMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.mapper.WmsZoneMapper;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsZone;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.dao.DuplicateKeyException;

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

	private final WmsLocationInventoryMapper locationInventoryMapper;

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
		if ("VIRTUAL".equalsIgnoreCase(zone.getZoneType())) {
			throw new BusinessException(400, "物理库位不能设置为虚拟库位");
		}

		List<Long> distinctIds = locationIds.stream().distinct().collect(Collectors.toList());
		List<WmsLocation> targets = wmsLocationMapper.selectPhysicalByIdsForUpdate(warehouseId, distinctIds);
		if (targets.size() != distinctIds.size()) {
			throw new BusinessException(400, "选中库位不存在、不属于该仓库或为虚拟库位");
		}
		java.util.Set<String> targetCodes = targets.stream().map(WmsLocation::getLocationCode)
				.collect(Collectors.toSet());
		List<String> occupiedCodes = locationInventoryMapper.listOccupiedLocationCodes(warehouseId).stream()
				.filter(targetCodes::contains)
				.collect(Collectors.toList());
		if (!occupiedCodes.isEmpty()) {
			throw new BusinessException(WmsResultCode.LOCATION_ZONE_MOVE_OCCUPIED.getCode(),
					WmsResultCode.LOCATION_ZONE_MOVE_OCCUPIED.getMessage() + "：" + String.join("、", occupiedCodes));
		}

		int updated = wmsLocationMapper.updateZoneBatch(warehouseId, distinctIds, zoneId);
		if (updated != distinctIds.size()) {
			throw new BusinessException(409, "库位分区更新数量不一致，请刷新后重试");
		}

		return updated;
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
		java.util.Set<String> existingTypes = listByWarehouse(warehouseId).stream()
				.map(WmsZone::getZoneType)
				.collect(java.util.stream.Collectors.toSet());
		int created = 0;
		for (String[] def : DEFAULT_ZONES) {
			if (existingTypes.contains(def[0])) {
				continue;
			}
			WmsZone zone = new WmsZone();
			zone.setWarehouseId(warehouseId);
			zone.setZoneType(def[0]);
			zone.setZoneName(def[1]);
			zone.setAllocatable(Integer.parseInt(def[2]));
			try {
				Assert.state(this.save(zone), "默认分区创建失败：" + def[1]);
				created++;
			}
			catch (DuplicateKeyException ignored) {
				// Another request filled this missing type after our initial read.
			}
		}
		return created;
	}

}
