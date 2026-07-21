package com.erp.admin.wms.controller;

import java.util.List;

import com.erp.admin.wms.model.vo.StorageOverviewVO;
import com.erp.admin.wms.service.WmsStorageOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务商「仓储概览」只读接口。作用域一律取 {@link com.erp.admin.common.tenant.WmsTenantContext}（登录服务商自身），
 * 不接受客户端传租户；权限 {@code wms:rack-inventory:read} 仅授服务商角色，货主/平台无此码。
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/storage-overview")
@Tag(name = "服务商仓储概览")
public class WmsStorageOverviewController {

	private final WmsStorageOverviewService storageOverviewService;

	@Operation(summary = "汇总 + 仓库列表（当前服务商）")
	@GetMapping("/summary")
	@PreAuthorize("@per.hasPermission('wms:rack-inventory:read')")
	public ApiResult<StorageOverviewVO> summary() {
		return ApiResult.ok(storageOverviewService.summary());
	}

	@Operation(summary = "某仓货架维度（当前服务商）")
	@GetMapping("/warehouse/{id}/racks")
	@PreAuthorize("@per.hasPermission('wms:rack-inventory:read')")
	public ApiResult<List<StorageOverviewVO.RackRow>> racks(@PathVariable("id") Long id) {
		return ApiResult.ok(storageOverviewService.racks(id));
	}

	@Operation(summary = "某仓货主维度（当前服务商）")
	@GetMapping("/warehouse/{id}/owners")
	@PreAuthorize("@per.hasPermission('wms:rack-inventory:read')")
	public ApiResult<List<StorageOverviewVO.OwnerRow>> owners(@PathVariable("id") Long id) {
		return ApiResult.ok(storageOverviewService.owners(id));
	}

}
