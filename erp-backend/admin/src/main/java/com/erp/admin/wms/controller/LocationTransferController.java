package com.erp.admin.wms.controller;

import java.util.List;

import com.erp.admin.wms.model.dto.LogicalLocationTransferCreateDTO;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.qo.LocationTransferQO;
import com.erp.admin.wms.model.vo.AvailableLocationVO;
import com.erp.admin.wms.model.vo.LocationTransferDetailVO;
import com.erp.admin.wms.model.vo.LocationTransferPageVO;
import com.erp.admin.wms.model.vo.LocationTransferSourceBatchVO;
import com.erp.admin.wms.model.vo.LogicalTransferLocationVO;
import com.erp.admin.wms.model.vo.LogicalTransferSourceVO;
import com.erp.admin.wms.service.LocationTransferOrderService;
import com.erp.admin.wms.service.LocationTransferService;
import com.erp.admin.wms.service.LogicalLocationTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.log.operation.annotation.OperationLog;
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
 * 库位调整单（库内移库单）。平台新建调整单→执行「调整完成」逐条移库→留痕。
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/location-transfer")
@Tag(name = "库位调整单")
public class LocationTransferController {

	private final LocationTransferOrderService orderService;

	private final LocationTransferService locationTransferService;

	private final LogicalLocationTransferService logicalTransferService;

	@Operation(summary = "库位调整单分页")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('wms:location:read')")
	public ApiResult<PageResult<LocationTransferPageVO>> page(PageParam pageParam, LocationTransferQO qo) {
		return ApiResult.ok(orderService.queryPage(pageParam, qo));
	}

	@Operation(summary = "库位调整单详情")
	@GetMapping("/detail")
	@PreAuthorize("@per.hasPermission('wms:location:read')")
	public ApiResult<LocationTransferDetailVO> detail(@RequestParam("id") Long id) {
		return ApiResult.ok(orderService.getDetail(id));
	}

	@Operation(summary = "新建逻辑库位调整单")
	@OperationLog(bizType = "库位调整", successMessage = "新建库位调整单成功")
	@PostMapping("/logical")
	@PreAuthorize("@per.hasPermission('wms:location:transfer')")
	public ApiResult<Long> createLogical(@Validated @RequestBody LogicalLocationTransferCreateDTO dto) {
		return ApiResult.ok(logicalTransferService.create(dto));
	}

	@Operation(summary = "完成逻辑库位调整")
	@OperationLog(bizType = "库位调整", successMessage = "库位调整完成")
	@PatchMapping("/logical-complete")
	@PreAuthorize("@per.hasPermission('wms:location:transfer')")
	public ApiResult<Void> completeLogical(@RequestParam("id") Long id) {
		logicalTransferService.complete(id);
		return ApiResult.ok();
	}

	@Operation(summary = "撤销库位调整单")
	@OperationLog(bizType = "库位调整", successMessage = "库位调整单已撤销")
	@PatchMapping("/cancel")
	@PreAuthorize("@per.hasPermission('wms:location:transfer')")
	public ApiResult<Void> cancel(@RequestParam("id") Long id) {
		orderService.cancel(id);
		return ApiResult.ok();
	}

	@Operation(summary = "删除库位调整单（仅已取消）")
	@OperationLog(bizType = "库位调整", successMessage = "库位调整单已删除")
	@DeleteMapping
	@PreAuthorize("@per.hasPermission('wms:location:transfer')")
	public ApiResult<Void> delete(@RequestBody List<Long> ids) {
		orderService.delete(ids);
		return ApiResult.ok();
	}

	@Operation(summary = "目标库位候选")
	@GetMapping("/candidates")
	@PreAuthorize("@per.hasPermission('wms:location:read')")
	public ApiResult<List<AvailableLocationVO>> candidates(
			@RequestParam("physicalInventoryId") Long physicalInventoryId,
			@RequestParam(value = "moveMode", required = false, defaultValue = "PARTIAL") String moveMode,
			@RequestParam(value = "targetLocationCode", required = false) String targetLocationCode) {
		return ApiResult.ok(locationTransferService.listCandidateTargets(
				physicalInventoryId, moveMode, targetLocationCode));
	}

	@Operation(summary = "按源库位查询可调整库存")
	@GetMapping("/sources")
	@PreAuthorize("@per.hasPermission('wms:location:read')")
	public ApiResult<List<LocationTransferSourceBatchVO>> sources(
			@RequestParam("warehouseId") Long warehouseId,
			@RequestParam(value = "locationCode", required = false) String locationCode,
			@RequestParam(value = "erpTenantId", required = false) Long erpTenantId,
			@RequestParam(value = "skuKeyword", required = false) String skuKeyword,
			@RequestParam(value = "palletNo", required = false) String palletNo) {
		return ApiResult.ok(locationTransferService.listSources(
				warehouseId, locationCode, erpTenantId, skuKeyword, palletNo));
	}

	@Operation(summary = "查询逻辑库位可调整库存")
	@GetMapping("/logical-sources")
	@PreAuthorize("@per.hasPermission('wms:location:read')")
	public ApiResult<List<LogicalTransferSourceVO>> logicalSources(
			@RequestParam("warehouseId") Long warehouseId,
			@RequestParam(value = "locationId", required = false) Long locationId,
			@RequestParam(value = "erpTenantId", required = false) Long erpTenantId,
			@RequestParam(value = "skuKeyword", required = false) String skuKeyword) {
		return ApiResult.ok(logicalTransferService.listSources(warehouseId, locationId, erpTenantId, skuKeyword));
	}

	@Operation(summary = "查询逻辑库位调整目标")
	@GetMapping("/logical-targets")
	@PreAuthorize("@per.hasPermission('wms:location:read')")
	public ApiResult<List<LogicalTransferLocationVO>> logicalTargets(
			@RequestParam("warehouseId") Long warehouseId,
			@RequestParam("erpTenantId") Long erpTenantId) {
		return ApiResult.ok(logicalTransferService.listTargets(warehouseId, erpTenantId));
	}

	@Operation(summary = "创建调整单可选库位（服务商自有物理库位及全部虚拟库位）")
	@GetMapping("/selectable-locations")
	@PreAuthorize("@per.hasPermission('wms:location:read')")
	public ApiResult<List<WmsLocation>> selectableLocations(
			@RequestParam("warehouseId") Long warehouseId,
			@RequestParam(value = "erpTenantId", required = false) Long erpTenantId,
			@RequestParam(value = "sourceLocationCode", required = false) String sourceLocationCode) {
		return ApiResult.ok(locationTransferService.listSelectableLocations(
				warehouseId, erpTenantId, sourceLocationCode));
	}

}
