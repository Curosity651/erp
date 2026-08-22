package com.erp.admin.wms.service;

import java.util.List;
import java.util.stream.Collectors;

import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.mapper.VirtualLocationReferenceMapper;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 虚拟库位管理（收纳积压货的通用化）。平台在库位管理里自定义增删多个虚拟库位。
 *
 * <p>虚拟库位不对应物理货架格子：排号为永不租出的合成值 {@link #RACK_VIRTUAL}，落在专用
 * VIRTUAL 分区（allocatable=1）。库存仍计入货主和服务商汇总，但具体虚拟位置只向海外仓平台展示；
 * 批次 container_stored=1 排除直接挑拣，搬运统一走库位调整。
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class VirtualLocationService {

	/** 虚拟库位分区类型。 */
	public static final String ZONE_VIRTUAL = "VIRTUAL";

	/** 虚拟库位专用排号——永不出现在 wms_rack_assignment，对所有服务商天然不可见。 */
	public static final String RACK_VIRTUAL = "VIRTUAL";

	private final WmsZoneService wmsZoneService;

	private final WmsLocationService wmsLocationService;

	private final WmsPhysicalInventoryService physicalInventoryService;

	private final VirtualLocationReferenceMapper virtualLocationReferenceMapper;

	private final WarehouseService warehouseService;

	/** 列出某仓库的虚拟库位。 */
	public List<WmsLocation> listVirtual(Long warehouseId) {
		warehouseService.validateOperableOwnWarehouse(warehouseId);
		return wmsLocationService.listByWarehouse(warehouseId).stream()
				.filter(l -> l.getIsVirtual() != null && l.getIsVirtual() == 1)
				.collect(Collectors.toList());
	}

	/**
	 * 新增一个虚拟库位（自定义名称）。名称即库位编码，仓内唯一。
	 * @param warehouseId 仓库ID
	 * @param name 虚拟库位名称/编码
	 * @return 新建的虚拟库位
	 */
	@Transactional(rollbackFor = Exception.class)
	public WmsLocation createVirtual(Long warehouseId, String name) {
		if (warehouseId == null) {
			throw new BusinessException(400, "仓库ID不能为空");
		}
		if (name == null || name.trim().isEmpty()) {
			throw new BusinessException(400, "虚拟库位名称不能为空");
		}
		warehouseService.validateOperableOwnWarehouse(warehouseId);
		String code = name.trim();
		boolean dup = wmsLocationService.listByWarehouse(warehouseId).stream()
				.anyMatch(l -> code.equals(l.getLocationCode()));
		if (dup) {
			throw new BusinessException(400, "该仓库已存在同名库位：" + code);
		}
		Long zoneId = ensureVirtualZone(warehouseId);
		WmsLocation loc = new WmsLocation();
		loc.setWarehouseId(warehouseId);
		loc.setZoneId(zoneId);
		loc.setRackNo(RACK_VIRTUAL);
		loc.setColumnNo(listVirtual(warehouseId).size() + 1);
		loc.setLocationCode(code);
		loc.setLocationType("BIG");
		loc.setPickType("STORE");
		loc.setIsVirtual(1);
		wmsLocationService.save(loc);
		return loc;
	}

	/**
	 * 删除虚拟库位（其上无货时才可删）。
	 * @param id 虚拟库位ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void deleteVirtual(Long id) {
		WmsLocation loc = wmsLocationService.getById(id);
		if (loc == null || loc.getIsVirtual() == null || loc.getIsVirtual() != 1) {
			throw new BusinessException(400, "虚拟库位不存在：" + id);
		}
		warehouseService.validateOperableOwnWarehouse(loc.getWarehouseId());
		boolean hasStock = physicalInventoryService.listAtLocation(loc.getWarehouseId(), loc.getLocationCode()).stream()
				.anyMatch(b -> (b.getQuantity() != null && b.getQuantity() > 0)
						|| (b.getReservedQty() != null && b.getReservedQty() > 0));
		if (hasStock) {
			throw new BusinessException(400, "该虚拟库位上仍有货，请先取回后再删除");
		}
		if (virtualLocationReferenceMapper.countUnfinishedTransfers(loc.getWarehouseId(), loc.getLocationCode()) > 0) {
			throw new BusinessException(400, "该虚拟库位存在未完成的库位调整单，不能删除");
		}
		if (virtualLocationReferenceMapper.countBusinessReferences(loc.getWarehouseId(), loc.getLocationCode()) > 0) {
			throw new BusinessException(400, "该虚拟库位仍被业务单据引用，不能删除");
		}
		if (wmsLocationService.deleteVirtualById(id, loc.getWarehouseId()) != 1) {
			throw new BusinessException(409, "虚拟库位已发生变化，请刷新后重试");
		}
	}

	/** 取（不存在则建）某仓库的虚拟分区ID。 */
	private Long ensureVirtualZone(Long warehouseId) {
		WmsZone zone = wmsZoneService.listByWarehouse(warehouseId).stream()
				.filter(z -> ZONE_VIRTUAL.equals(z.getZoneType()))
				.findFirst().orElse(null);
		if (zone != null) {
			return zone.getId();
		}
		WmsZone nz = new WmsZone();
		nz.setWarehouseId(warehouseId);
		nz.setZoneType(ZONE_VIRTUAL);
		nz.setZoneName("虚拟库位区");
		// allocatable=1：入库位后批次保持 allocatable=1 → 货主可用不变(甲)；不参与自动挑拣由 container_stored 承担。
		nz.setAllocatable(1);
		wmsZoneService.save(nz);
		return nz.getId();
	}

}
