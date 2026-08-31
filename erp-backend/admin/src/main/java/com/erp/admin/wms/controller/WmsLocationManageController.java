package com.erp.admin.wms.controller;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.erp.admin.wms.model.dto.LogicalLocationCreateDTO;
import com.erp.admin.wms.model.dto.LogicalLocationUpdateDTO;
import com.erp.admin.wms.model.dto.MoveLocationZoneDTO;
import com.erp.admin.wms.model.dto.WarehousePalletRuleDTO;
import com.erp.admin.wms.model.dto.WarehouseStructureDTO;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.vo.WarehouseStructureVO;
import com.erp.admin.wms.model.vo.WarehouseLocationSummaryVO;
import com.erp.admin.wms.model.vo.LocationSlotSummaryVO;
import com.erp.admin.wms.config.WmsCoreModeGuard;
import com.erp.admin.wms.service.WarehouseService;
import com.erp.admin.wms.service.WmsLocationGenerator;
import com.erp.admin.wms.service.WmsLocationService;
import com.erp.admin.wms.service.LocationInventoryService;
import com.erp.admin.wms.service.VirtualLocationService;
import com.erp.admin.wms.service.WmsStructureLockService;
import com.erp.admin.wms.service.WmsZoneService;
import com.erp.admin.wms.service.WmsPalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

	private final LocationInventoryService locationInventoryService;

	private final VirtualLocationService virtualLocationService;

	private final WmsPalletService wmsPalletService;

	private final WmsCoreModeGuard coreModeGuard;

	@Operation(summary = "自有仓库列表（含结构参数）")
	@GetMapping("/warehouses")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<List<WarehouseStructureVO>> warehouses() {
		List<Warehouse> warehouses = warehouseService.listOwnWarehouses(null);
		Map<Long, WarehouseLocationSummaryVO> summaries = wmsLocationService
			.summarizeByWarehouseIds(warehouses.stream().map(Warehouse::getId).collect(Collectors.toList()))
			.stream().collect(Collectors.toMap(WarehouseLocationSummaryVO::getWarehouseId, row -> row));
		List<WarehouseStructureVO> list = warehouses.stream().map(w -> {
			WarehouseStructureVO vo = new WarehouseStructureVO();
			WarehouseLocationSummaryVO summary = summaries.get(w.getId());
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
			vo.setPalletPositionsPerLevel(w.getPalletPositionsPerLevel());
			vo.setMaxSkuKindsPerPallet(w.getMaxSkuKindsPerPallet());
			vo.setAllowCrossOwnerMix(w.getAllowCrossOwnerMix());
			vo.setDefaultPalletLengthMm(w.getDefaultPalletLengthMm());
			vo.setDefaultPalletWidthMm(w.getDefaultPalletWidthMm());
			vo.setDefaultPalletHeightMm(w.getDefaultPalletHeightMm());
			vo.setDefaultPalletMaxWeightKg(w.getDefaultPalletMaxWeightKg());
			vo.setDefaultPalletUtilization(w.getDefaultPalletUtilization());
			int actualLocationCount = summary == null || summary.getActualLocationCount() == null
					? 0 : summary.getActualLocationCount();
			vo.setActualPhysicalLocationCount(actualLocationCount);
			vo.setActualRackCount(summary == null || summary.getActualRackCount() == null
					? 0 : summary.getActualRackCount());
			vo.setAssignableRackCount(summary == null || summary.getAssignableRackCount() == null
					? 0 : summary.getAssignableRackCount());
			vo.setLocationConfigured(actualLocationCount > 0);
			vo.setActualPalletSlotCount(wmsLocationGenerator.countPhysicalSlots(w.getId()));
			return vo;
		}).collect(Collectors.toList());
		return ApiResult.ok(list);
	}

	@Operation(summary = "保存仓库结构并原子生成库位")
	@PatchMapping("/structure")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Integer> updateStructure(@Validated @RequestBody WarehouseStructureDTO dto) {
		coreModeGuard.assertLegacyWriteAllowed("仓库结构生成");
		return ApiResult.ok(wmsLocationGenerator.saveStructureAndGenerate(dto));
	}

	@Operation(summary = "更新托盘规则（不重新生成库位）")
	@PatchMapping("/pallet-rules")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Integer> updatePalletRules(@Validated @RequestBody WarehousePalletRuleDTO dto) {
		coreModeGuard.assertLegacyWriteAllowed("托盘规则修改");
		return ApiResult.ok(warehouseService.updatePalletRules(dto));
	}

	@Operation(summary = "仓库分区列表")
	@GetMapping("/zones")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<List<WmsZone>> zones(@RequestParam("warehouseId") Long warehouseId) {
		warehouseService.validateOperableOwnWarehouse(warehouseId);
		return ApiResult.ok(wmsZoneService.listByWarehouse(warehouseId));
	}

	@Operation(summary = "初始化默认四类分区")
	@PostMapping("/zones/init-defaults")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Integer> initDefaultZones(@RequestParam("warehouseId") Long warehouseId) {
		warehouseService.validateOperableOwnWarehouse(warehouseId);
		return ApiResult.ok(wmsZoneService.initDefaultZones(warehouseId));
	}

	@Operation(summary = "批量生成库位")
	@PostMapping("/generate")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Integer> generate(@RequestParam("warehouseId") Long warehouseId) {
		coreModeGuard.assertLegacyWriteAllowed("批量生成几何库位");
		return ApiResult.ok(wmsLocationGenerator.generateLocations(warehouseId));
	}

	@Operation(summary = "仓库库位列表")
	@GetMapping("/locations")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<List<WmsLocation>> locations(@RequestParam("warehouseId") Long warehouseId) {
		warehouseService.validateOperableOwnWarehouse(warehouseId);
		return ApiResult.ok(wmsLocationService.listPhysicalByWarehouse(warehouseId));
	}

	@Operation(summary = "按排查询逻辑库位")
	@GetMapping("/locations/grouped")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Map<String, List<WmsLocation>>> groupedLocations(@RequestParam("warehouseId") Long warehouseId) {
		warehouseService.validateOperableOwnWarehouse(warehouseId);
		Map<String, List<WmsLocation>> grouped = wmsLocationService.listByWarehouse(warehouseId).stream()
			.collect(Collectors.groupingBy(WmsLocation::getRackNo, LinkedHashMap::new, Collectors.toList()));
		return ApiResult.ok(grouped);
	}

	@Operation(summary = "新增逻辑库位")
	@PostMapping("/locations")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Long> createLocation(@Validated @RequestBody LogicalLocationCreateDTO dto) {
		warehouseService.validateOperableOwnWarehouse(dto.getWarehouseId());
		return ApiResult.ok(wmsLocationService.createLocation(dto));
	}

	@Operation(summary = "修改逻辑库位属性")
	@PutMapping("/locations/{id}")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Void> updateLocation(@PathVariable("id") Long id,
			@Validated @RequestBody LogicalLocationUpdateDTO dto) {
		WmsLocation location = wmsLocationService.getById(id);
		org.springframework.util.Assert.notNull(location, "库位不存在");
		warehouseService.validateOperableOwnWarehouse(location.getWarehouseId());
		wmsLocationService.updateLocation(id, dto);
		return ApiResult.ok();
	}

	@Operation(summary = "删除空逻辑库位")
	@DeleteMapping("/locations/{id}")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Void> deleteLocation(@PathVariable("id") Long id) {
		WmsLocation location = wmsLocationService.getById(id);
		org.springframework.util.Assert.notNull(location, "库位不存在");
		warehouseService.validateOperableOwnWarehouse(location.getWarehouseId());
		wmsLocationService.deleteEmptyLocation(id);
		return ApiResult.ok();
	}

	@Operation(summary = "该仓有货占用的库位编码集合（前端锁定有货格子改分区用）")
	@GetMapping("/occupied-locations")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<List<String>> occupiedLocations(@RequestParam("warehouseId") Long warehouseId) {
		warehouseService.validateOperableOwnWarehouse(warehouseId);
		return ApiResult.ok(locationInventoryService.occupiedLocationCodes(warehouseId));
	}

	@Operation(summary = "库位改分区（联动更新现有批次可分配性并重算可用库存）")
	@PostMapping("/locations/move-zone")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Integer> moveLocationZone(@Validated @RequestBody MoveLocationZoneDTO dto) {
		warehouseService.validateOperableOwnWarehouse(dto.getWarehouseId());
		return ApiResult.ok(wmsZoneService.moveLocationsToZone(dto.getWarehouseId(), dto.getLocationIds(), dto.getZoneId()));
	}

	@GetMapping("/slot-summary")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<List<LocationSlotSummaryVO>> slotSummary(@RequestParam("warehouseId") Long warehouseId) {
		warehouseService.validateOperableOwnWarehouse(warehouseId);
		return ApiResult.ok(wmsPalletService.listSlotSummaries(warehouseId));
	}

	@GetMapping("/structure-lock")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<WmsStructureLockService.LockInfo> structureLock(@RequestParam("warehouseId") Long warehouseId) {
		warehouseService.validateOperableOwnWarehouse(warehouseId);
		return ApiResult.ok(wmsStructureLockService.compute(warehouseId));
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
