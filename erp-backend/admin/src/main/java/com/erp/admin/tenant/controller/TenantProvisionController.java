package com.erp.admin.tenant.controller;

import java.util.List;

import com.erp.admin.tenant.model.dto.OpenTenantDTO;
import com.erp.admin.tenant.model.qo.TenantQO;
import com.erp.admin.tenant.model.vo.TenantBriefVO;
import com.erp.admin.tenant.service.TenantProvisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 双层租户开通接口（B3）。
 *
 * <ul>
 * <li>平台超管：开通 / 查看 WMS 服务商。</li>
 * <li>WMS 服务商：开通 / 查看自己名下的货主。</li>
 * </ul>
 *
 * 权限由 {@link TenantProvisionService} 按当前身份校验（非超管不能开服务商；非服务商不能开货主）。
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/tenant")
@Tag(name = "租户开通")
public class TenantProvisionController {

	private final TenantProvisionService tenantProvisionService;

	@Operation(summary = "平台超管开通 WMS 服务商")
	@PostMapping("/wms-operators")
	public ApiResult<Long> openWmsOperator(@RequestBody OpenTenantDTO dto) {
		return ApiResult.ok(this.tenantProvisionService.openWmsOperator(dto));
	}

	@Operation(summary = "WMS 服务商列表（平台超管）")
	@GetMapping("/wms-operators")
	public ApiResult<List<TenantBriefVO>> listWmsOperators() {
		return ApiResult.ok(this.tenantProvisionService.listWmsOperators());
	}

	@Operation(summary = "WMS 服务商分页（平台超管）")
	@GetMapping("/wms-operators/page")
	public ApiResult<PageResult<TenantBriefVO>> pageWmsOperators(PageParam pageParam, TenantQO qo) {
		return ApiResult.ok(this.tenantProvisionService.pageWmsOperators(pageParam, qo));
	}

	@Operation(summary = "启用/停用 WMS 服务商（平台超管，停用级联其名下货主）")
	@PutMapping("/wms-operators/{id}/status")
	public ApiResult<Void> setOperatorStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status) {
		this.tenantProvisionService.setOperatorStatus(id, status);
		return ApiResult.ok();
	}

	@Operation(summary = "某 WMS 服务商名下货主（平台超管，详情下钻）")
	@GetMapping("/wms-operators/{id}/erp-tenants")
	public ApiResult<List<TenantBriefVO>> listErpTenantsByParent(@PathVariable("id") Long id) {
		return ApiResult.ok(this.tenantProvisionService.listErpTenantsByParent(id));
	}

	@Operation(summary = "WMS 服务商开通货主")
	@PostMapping("/erp-tenants")
	public ApiResult<Long> openErpTenant(@RequestBody OpenTenantDTO dto) {
		return ApiResult.ok(this.tenantProvisionService.openErpTenant(dto));
	}

	@Operation(summary = "货主列表（当前 WMS 服务商名下）")
	@GetMapping("/erp-tenants")
	public ApiResult<List<TenantBriefVO>> listErpTenants() {
		return ApiResult.ok(this.tenantProvisionService.listErpTenants());
	}

	@Operation(summary = "全部货主列表（平台超管，含所属服务商，报废单等平台侧下拉用）")
	@GetMapping("/all-erp-tenants")
	public ApiResult<List<TenantBriefVO>> listAllErpTenants() {
		return ApiResult.ok(this.tenantProvisionService.listAllErpTenants());
	}

	@Operation(summary = "货主分页（当前 WMS 服务商名下）")
	@GetMapping("/erp-tenants/page")
	public ApiResult<PageResult<TenantBriefVO>> pageErpTenants(PageParam pageParam, TenantQO qo) {
		return ApiResult.ok(this.tenantProvisionService.pageErpTenants(pageParam, qo));
	}

	@Operation(summary = "启用/停用货主（当前服务商名下，仅控账号开通停用）")
	@PutMapping("/erp-tenants/{id}/status")
	public ApiResult<Void> setErpTenantStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status) {
		this.tenantProvisionService.setErpTenantStatus(id, status);
		return ApiResult.ok();
	}

}
