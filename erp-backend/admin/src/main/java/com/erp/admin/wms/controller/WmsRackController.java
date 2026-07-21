package com.erp.admin.wms.controller;

import java.util.List;
import java.util.stream.Collectors;

import com.erp.admin.tenant.enums.TenantType;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.enums.WmsResultCode;
import com.erp.admin.wms.model.dto.RackAssignDTO;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.vo.RackVO;
import com.erp.admin.wms.model.vo.WmsOperatorOptionVO;
import com.erp.admin.wms.service.WmsLocationService;
import com.erp.admin.wms.service.WmsRackAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 货架分配 + 二维预览（C2）。分配/解除仅海外仓平台;预览/下钻沿用 wms:warehouse 权限。
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/rack")
@Tag(name = "货架分配")
public class WmsRackController {

	private final WmsRackAssignmentService rackAssignmentService;

	private final WmsLocationService wmsLocationService;

	private final TenantIdentityService tenantIdentityService;

	private final SysTenantMapper sysTenantMapper;

	// 货架分配为海外仓平台专属功能：预览会暴露各服务商的货架归属/月租费，选项会暴露服务商名单，
	// 故读接口一律锁平台（wms:warehouse:edit 平台专属 + 身份兜底），不再用共享的 wms:warehouse:read
	// （该码为货主/服务商的仓库下拉共享，会导致跨租户商业信息泄漏）。
	@Operation(summary = "仓库二维预览(按排，仅海外仓平台)")
	@GetMapping("/preview")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<List<RackVO>> preview(@RequestParam("warehouseId") Long warehouseId,
			@RequestParam(value = "wmsTenantId", required = false) Long wmsTenantId,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "expiringSoon", required = false) Boolean expiringSoon) {
		assertOverseasPlatform();
		return ApiResult.ok(rackAssignmentService.preview(warehouseId, wmsTenantId, status, expiringSoon));
	}

	@Operation(summary = "分配货架(仅海外仓平台)")
	@PostMapping("/assign")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Integer> assign(@Validated @RequestBody RackAssignDTO dto) {
		assertOverseasPlatform();
		return ApiResult.ok(rackAssignmentService.assign(dto));
	}

	@Operation(summary = "解除分配(仅海外仓平台)")
	@DeleteMapping("/{id}")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Void> unassign(@PathVariable("id") Long id) {
		assertOverseasPlatform();
		rackAssignmentService.unassign(id);
		return ApiResult.ok();
	}

	@Operation(summary = "某排库位下钻(仅海外仓平台)")
	@GetMapping("/locations")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<List<WmsLocation>> rackLocations(@RequestParam("warehouseId") Long warehouseId,
			@RequestParam("rackNo") String rackNo) {
		assertOverseasPlatform();
		List<WmsLocation> list = wmsLocationService.listByWarehouse(warehouseId).stream()
			.filter(l -> rackNo.equals(l.getRackNo())).collect(Collectors.toList());
		return ApiResult.ok(list);
	}

	@Operation(summary = "WMS服务商选项(仅海外仓平台)")
	@GetMapping("/operators")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<List<WmsOperatorOptionVO>> operators() {
		assertOverseasPlatform();
		List<SysTenant> list = sysTenantMapper.selectList(WrappersX.lambdaQueryX(SysTenant.class)
			.eq(SysTenant::getTenantType, TenantType.WMS_OPERATOR.name())
			.orderByDesc(SysTenant::getId));
		List<WmsOperatorOptionVO> options = list.stream().map(t -> {
			WmsOperatorOptionVO vo = new WmsOperatorOptionVO();
			vo.setId(t.getId());
			vo.setName(t.getTenantName());
			return vo;
		}).collect(Collectors.toList());
		return ApiResult.ok(options);
	}

	private void assertOverseasPlatform() {
		String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
		if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identityType)) {
			throw new BusinessException(WmsResultCode.RACK_ASSIGN_FORBIDDEN.getCode(),
					WmsResultCode.RACK_ASSIGN_FORBIDDEN.getMessage());
		}
	}

}
