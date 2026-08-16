package com.erp.admin.wms.controller;

import java.util.List;

import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.enums.WmsResultCode;
import com.erp.admin.wms.config.WmsCoreModeGuard;
import com.erp.admin.wms.model.dto.PutawayDTO;
import com.erp.admin.wms.model.entity.Inventory;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.service.ErpOwnerScopeService;
import com.erp.admin.wms.service.WmsPhysicalInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.BeanUtils;
import java.util.stream.Collectors;

/**
 * 批次级库存 SSOT（D1·方案②）。上架写入仅海外仓平台（物理作业归平台）；批次/快照查询沿用 wms 读权限。
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/physical-inventory")
@Tag(name = "批次级库存SSOT")
public class WmsPhysicalInventoryController {

	private final WmsPhysicalInventoryService physicalInventoryService;

	private final TenantIdentityService tenantIdentityService;

	private final ErpOwnerScopeService erpOwnerScopeService;

	private final WmsCoreModeGuard coreModeGuard;

	@Operation(summary = "上架写入批次(仅海外仓平台)")
	@PostMapping("/putaway")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<WmsPhysicalInventory> putaway(@Validated @RequestBody PutawayDTO dto) {
		coreModeGuard.assertLegacyWriteAllowed("旧物理批次上架");
		assertOverseasPlatform();
		return ApiResult.ok(physicalInventoryService.putaway(dto));
	}

	@Operation(summary = "批次明细(按货主只读)")
	@GetMapping("/batches")
	@PreAuthorize("@per.hasPermission('wms:inventory:read')")
	public ApiResult<List<WmsPhysicalInventory>> batches(@RequestParam("erpTenantId") Long erpTenantId,
			@RequestParam(value = "warehouseId", required = false) Long warehouseId,
			@RequestParam(value = "skuKeyword", required = false) String skuKeyword) {
		assertCanAccessOwner(erpTenantId);
		List<WmsPhysicalInventory> batches =
				physicalInventoryService.listByErpTenant(erpTenantId, warehouseId, skuKeyword);
		if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM
				.equals(tenantIdentityService.currentIdentity(null).getIdentityType())) {
			batches = batches.stream().map(this::withoutLocationDetails).collect(Collectors.toList());
		}
		return ApiResult.ok(batches);
	}

	@Operation(summary = "聚合桶快照(对账)")
	@GetMapping("/snapshot")
	@PreAuthorize("@per.hasPermission('wms:inventory:read')")
	public ApiResult<Inventory> snapshot(@RequestParam(value = "wmsTenantId", required = false) Long wmsTenantId,
			@RequestParam("erpTenantId") Long erpTenantId, @RequestParam("warehouseId") Long warehouseId,
			@RequestParam("skuCode") String skuCode) {
		assertCanAccessOwner(erpTenantId);
		return ApiResult.ok(physicalInventoryService.snapshot(wmsTenantId, erpTenantId, warehouseId, skuCode));
	}

	private void assertOverseasPlatform() {
		String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
		if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identityType)) {
			throw new BusinessException(WmsResultCode.RACK_ASSIGN_FORBIDDEN.getCode(), "仅海外仓平台可执行上架作业");
		}
	}

	/**
	 * 数据级作用域校验:平台放行全部;货主仅限自身、服务商仅限名下货主。
	 * 防止持 wms:inventory:read 的货主/服务商以任意 erpTenantId 越权读取他人批次/快照。
	 */
	private void assertCanAccessOwner(Long erpTenantId) {
		if (!erpOwnerScopeService.canAccess(erpTenantId)) {
			throw new BusinessException(WmsResultCode.RACK_ASSIGN_FORBIDDEN.getCode(), "无权查看该货主的库存");
		}
	}

	private WmsPhysicalInventory withoutLocationDetails(WmsPhysicalInventory source) {
		WmsPhysicalInventory copy = new WmsPhysicalInventory();
		BeanUtils.copyProperties(source, copy);
		copy.setLocationCode(null);
		copy.setOriginLocationCode(null);
		copy.setPalletId(null);
		copy.setSlotId(null);
		copy.setZoneId(null);
		copy.setContainerStored(null);
		return copy;
	}

}
