package com.erp.admin.wms.service;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.wms.mapper.WmsLocationInventoryMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.dto.LogicalLocationCreateDTO;
import com.erp.admin.wms.model.dto.LogicalLocationUpdateDTO;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsLocationInventory;
import lombok.RequiredArgsConstructor;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 库位服务（C1）。
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class WmsLocationService extends ExtendServiceImpl<WmsLocationMapper, WmsLocation> {

	private final WmsLocationInventoryMapper locationInventoryMapper;

	@Transactional(rollbackFor = Exception.class)
	public Long createLocation(LogicalLocationCreateDTO dto) {
		validateIdentity(dto.getWarehouseId(), dto.getRackNo(), dto.getSequenceNo(), dto.getLocationCode());
		WmsLocation location = new WmsLocation();
		location.setWarehouseId(dto.getWarehouseId());
		location.setZoneId(dto.getZoneId());
		location.setRackNo(dto.getRackNo().trim());
		location.setColumnNo(dto.getSequenceNo());
		location.setLocationCode(dto.getLocationCode().trim());
		location.setLocationType(dto.getLocationType());
		location.setPickType("PICK");
		location.setIsVirtual(0);
		copyMutableFields(location, dto.getZoneId(), dto.getLocationType(), dto.getLengthMm(), dto.getWidthMm(),
				dto.getHeightMm(), dto.getMaxWeightKg(), dto.getMaxSkuKinds(), dto.getPublicShared());
		Assert.isTrue(baseMapper.insert(location) == 1, "库位创建失败，请重试");
		return location.getId();
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateLocation(Long locationId, LogicalLocationUpdateDTO dto) {
		WmsLocation current = baseMapper.selectLogicalByIdForUpdate(locationId);
		Assert.notNull(current, "库位不存在");
		WmsLocation update = new WmsLocation();
		update.setId(locationId);
		copyMutableFields(update, dto.getZoneId(), dto.getLocationType(), dto.getLengthMm(), dto.getWidthMm(),
				dto.getHeightMm(), dto.getMaxWeightKg(), dto.getMaxSkuKinds(), dto.getPublicShared());
		Assert.isTrue(baseMapper.updateById(update) == 1, "库位更新失败，请重试");
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteEmptyLocation(Long locationId) {
		WmsLocation location = baseMapper.selectLogicalByIdForUpdate(locationId);
		Assert.notNull(location, "库位不存在");
		long blocking = locationInventoryMapper.selectCount(Wrappers.<WmsLocationInventory>lambdaQuery()
			.eq(WmsLocationInventory::getLocationId, locationId)
			.and(query -> query.gt(WmsLocationInventory::getQuantity, 0)
				.or()
				.gt(WmsLocationInventory::getReservedQuantity, 0)));
		Assert.isTrue(blocking == 0, "库位仍有库存或预占，不能删除");
		Assert.isTrue(baseMapper.deleteById(locationId) == 1, "库位删除失败，请刷新后重试");
	}

	private void validateIdentity(Long warehouseId, String rackNo, Integer sequenceNo, String locationCode) {
		Assert.notNull(warehouseId, "仓库不能为空");
		Assert.hasText(rackNo, "排号不能为空");
		Assert.isTrue(sequenceNo != null && sequenceNo > 0, "排内顺序必须大于0");
		Assert.hasText(locationCode, "库位编号不能为空");
		Assert.isTrue(baseMapper.countIncludingDeletedByCode(warehouseId, locationCode.trim()) == 0,
				"库位编号已存在或曾经使用");
		Assert.isTrue(baseMapper.countIncludingDeletedBySequence(warehouseId, rackNo.trim(), sequenceNo) == 0,
				"该排内顺序已存在或曾经使用");
	}

	private void copyMutableFields(WmsLocation location, Long zoneId, String locationType, Integer lengthMm,
			Integer widthMm, Integer heightMm, java.math.BigDecimal maxWeightKg, Integer maxSkuKinds,
			Integer publicShared) {
		Assert.notNull(zoneId, "库位分区不能为空");
		Assert.hasText(locationType, "库位类型不能为空");
		Assert.isTrue(lengthMm != null && lengthMm > 0, "库位长度必须大于0");
		Assert.isTrue(widthMm != null && widthMm > 0, "库位宽度必须大于0");
		Assert.isTrue(heightMm != null && heightMm > 0, "库位高度必须大于0");
		Assert.isTrue(maxWeightKg != null && maxWeightKg.signum() > 0, "最大承重必须大于0");
		Assert.isTrue(maxSkuKinds != null && maxSkuKinds >= 0, "最大SKU种类数不能小于0");
		Assert.isTrue(publicShared != null && (publicShared == 0 || publicShared == 1), "公共共享标记不正确");
		location.setZoneId(zoneId);
		location.setLocationType(locationType.trim());
		location.setLengthMm(lengthMm);
		location.setWidthMm(widthMm);
		location.setHeightMm(heightMm);
		location.setMaxWeightKg(maxWeightKg);
		location.setMaxSkuKinds(maxSkuKinds);
		location.setPublicShared(publicShared);
	}

	public List<WmsLocation> listByWarehouse(Long warehouseId) {
		return baseMapper.listByWarehouse(warehouseId);
	}

	public List<WmsLocation> listPhysicalByWarehouse(Long warehouseId) {
		return baseMapper.listPhysicalByWarehouse(warehouseId);
	}

	public long countByWarehouse(Long warehouseId) {
		return baseMapper.countByWarehouse(warehouseId);
	}

	public int deleteByWarehouse(Long warehouseId) {
		return baseMapper.deleteByWarehouse(warehouseId);
	}

	public int deletePhysicalByWarehouse(Long warehouseId) {
		return baseMapper.deletePhysicalByWarehouse(warehouseId);
	}

	public long countPhysicalByWarehouse(Long warehouseId) {
		return baseMapper.countPhysicalByWarehouse(warehouseId);
	}

}
