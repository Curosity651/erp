package com.erp.admin.wms.controller;

import java.util.List;
import java.util.stream.Collectors;

import com.erp.admin.wms.model.dto.MoveLocationZoneDTO;
import com.erp.admin.wms.model.dto.WarehouseStructureDTO;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.vo.WarehouseStructureVO;
import com.erp.admin.wms.service.WarehouseService;
import com.erp.admin.wms.service.WmsLocationGenerator;
import com.erp.admin.wms.service.WmsLocationService;
import com.erp.admin.wms.service.VirtualLocationService;
import com.erp.admin.wms.service.WmsPhysicalInventoryService;
import com.erp.admin.wms.service.WmsStructureLockService;
import com.erp.admin.wms.service.WmsZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库位管理（C1）：仓库结构参数维护、品质分区初始化、库位批量生成与查询。
 *
 * <p>沿用 WMS 现有权限码 {@code wms:warehouse:read/edit}（已授 ROLE_ADMIN）。
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/location-mgmt")
@Tag(name = "库位管理")
public class WmsLocationManageController {

	private final WarehouseService warehouseService;

	private final WmsZoneService wmsZoneService;

	private final WmsLocationService wmsLocationService;

	private final WmsLocationGenerator wmsLocationGenerator;

	private final WmsStructureLockService wmsStructureLockService;

	private final WmsPhysicalInventoryService wmsPhysicalInventoryService;

	private final VirtualLocationService virtualLocationService;

	@Operation(summary = "自有仓库列表（含结构参数）")
	@GetMapping("/warehouses")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<List<WarehouseStructureVO>> warehouses() {
		List<WarehouseStructureVO> list = warehouseService.listOwnWarehouses(null).stream().map(w -> {
			WarehouseStructureVO vo = new WarehouseStructureVO();
			vo.setId(w.getId());
			vo.setWarehouseCode(w.getWarehouseCode());
			vo.setWarehouseName(w.getWarehouseName());
			vo.setWarehouseType(w.getWarehouseType());
			vo.setRackRows(w.getRackRows());
			vo.setRackColumns(w.getRackColumns());
			vo.setRackNoPrefix(w.getRackNoPrefix());
			vo.setCodePadWidth(w.getCodePadWidth());
			vo.setDefaultLocationType(w.getDefaultLocationType());
			vo.setLocationGenerated(w.getLocationGenerated());
			vo.setPalletLevels(w.getPalletLevels());
			vo.setMaxSkuKindsPerPallet(w.getMaxSkuKindsPerPallet());
			vo.setAllowCrossOwnerMix(w.getAllowCrossOwnerMix());
			vo.setDefaultPalletLengthMm(w.getDefaultPalletLengthMm());
			vo.setDefaultPalletWidthMm(w.getDefaultPalletWidthMm());
			vo.setDefaultPalletHeightMm(w.getDefaultPalletHeightMm());
			vo.setDefaultPalletMaxWeightKg(w.getDefaultPalletMaxWeightKg());
			vo.setDefaultPalletUtilization(w.getDefaultPalletUtilization());
			// 结构锁定状态（有货占用 / 已分配服务商 → 前端置灰保存结构·重新生成）
			WmsStructureLockService.LockInfo lock = wmsStructureLockService.compute(w.getId());
			vo.setStructureLocked(lock.isLocked());
			vo.setOccupied(lock.occupied);
			vo.setOccupiedLocationCount(lock.occupiedLocationCount);
			vo.setAssigned(lock.assigned);
			vo.setAssignedRackCount(lock.assignedRackCount);
			vo.setAssignedOperatorNames(lock.assignedOperatorNames);
			return vo;
		}).collect(Collectors.toList());
		return ApiResult.ok(list);
	}

	@Operation(summary = "更新仓库结构参数")
	@PatchMapping("/structure")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Void> updateStructure(@Validated @RequestBody WarehouseStructureDTO dto) {
		// 硬闸：有货占用或已分配服务商时禁改结构（与前端置灰同口径，防绕过）
		wmsStructureLockService.assertEditable(dto.getId());
		Warehouse wh = new Warehouse();
		wh.setId(dto.getId());
		wh.setRackRows(dto.getRackRows());
		wh.setRackColumns(dto.getRackColumns());
		wh.setRackNoPrefix(dto.getRackNoPrefix() == null ? "" : dto.getRackNoPrefix());
		wh.setCodePadWidth(dto.getCodePadWidth());
		wh.setDefaultLocationType(dto.getDefaultLocationType());
		wh.setPalletLevels(dto.getPalletLevels() == null ? 3 : dto.getPalletLevels());
		wh.setMaxSkuKindsPerPallet(dto.getMaxSkuKindsPerPallet() == null ? 4 : dto.getMaxSkuKindsPerPallet());
		wh.setAllowCrossOwnerMix(dto.getAllowCrossOwnerMix() == null ? 1 : dto.getAllowCrossOwnerMix());
		wh.setDefaultPalletLengthMm(dto.getDefaultPalletLengthMm());
		wh.setDefaultPalletWidthMm(dto.getDefaultPalletWidthMm());
		wh.setDefaultPalletHeightMm(dto.getDefaultPalletHeightMm());
		wh.setDefaultPalletMaxWeightKg(dto.getDefaultPalletMaxWeightKg());
		wh.setDefaultPalletUtilization(dto.getDefaultPalletUtilization());
		warehouseService.updateById(wh);
		return ApiResult.ok();
	}

	@Operation(summary = "仓库分区列表")
	@GetMapping("/zones")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<List<WmsZone>> zones(@RequestParam("warehouseId") Long warehouseId) {
		return ApiResult.ok(wmsZoneService.listByWarehouse(warehouseId));
	}

	@Operation(summary = "初始化默认四类分区")
	@PostMapping("/zones/init-defaults")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Integer> initDefaultZones(@RequestParam("warehouseId") Long warehouseId) {
		return ApiResult.ok(wmsZoneService.initDefaultZones(warehouseId));
	}

	@Operation(summary = "批量生成库位")
	@PostMapping("/generate")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Integer> generate(@RequestParam("warehouseId") Long warehouseId) {
		return ApiResult.ok(wmsLocationGenerator.generateLocations(warehouseId));
	}

	@Operation(summary = "仓库库位列表")
	@GetMapping("/locations")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<List<WmsLocation>> locations(@RequestParam("warehouseId") Long warehouseId) {
		return ApiResult.ok(wmsLocationService.listByWarehouse(warehouseId));
	}

	@Operation(summary = "该仓有货占用的库位编码集合（前端锁定有货格子改分区用）")
	@GetMapping("/occupied-locations")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<List<String>> occupiedLocations(@RequestParam("warehouseId") Long warehouseId) {
		return ApiResult.ok(wmsPhysicalInventoryService.occupiedLocationCodes(warehouseId));
	}

	@Operation(summary = "库位改分区（联动更新现有批次可分配性并重算可用库存）")
	@PostMapping("/locations/move-zone")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Integer> moveLocationZone(@Validated @RequestBody MoveLocationZoneDTO dto) {
		return ApiResult.ok(wmsZoneService.moveLocationsToZone(dto.getWarehouseId(), dto.getLocationIds(), dto.getZoneId()));
	}

	// ==================== 虚拟库位（收纳积压货：服务商不可见、货主数量不变、不参与自动发货） ====================

	@Operation(summary = "虚拟库位列表")
	@GetMapping("/virtual-locations")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<List<WmsLocation>> virtualLocations(@RequestParam("warehouseId") Long warehouseId) {
		return ApiResult.ok(virtualLocationService.listVirtual(warehouseId));
	}

	@Operation(summary = "新增虚拟库位")
	@PostMapping("/virtual-locations")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<WmsLocation> createVirtualLocation(@RequestParam("warehouseId") Long warehouseId,
			@RequestParam("name") String name) {
		return ApiResult.ok(virtualLocationService.createVirtual(warehouseId, name));
	}

	@Operation(summary = "删除虚拟库位（无货时）")
	@DeleteMapping("/virtual-locations")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Void> deleteVirtualLocation(@RequestParam("id") Long id) {
		virtualLocationService.deleteVirtual(id);
		return ApiResult.ok();
	}

}
