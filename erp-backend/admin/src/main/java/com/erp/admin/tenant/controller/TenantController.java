package com.erp.admin.tenant.controller;

import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户身份接口（B2）。任意已登录用户可访问，用于前端两入口登录校验、菜单裁剪与身份展示。
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/tenant")
@Tag(name = "租户身份")
public class TenantController {

	private final TenantIdentityService tenantIdentityService;

	/**
	 * 取当前登录用户的租户身份；可选校验登录入口是否匹配。
	 * @param expectType 前端选择的入口类型（WMS_OPERATOR/ERP_USER），不传则不校验
	 * @return 当前租户身份
	 */
	@Operation(summary = "当前租户身份（含入口校验）")
	@GetMapping("/current")
	public ApiResult<TenantIdentityVO> current(@RequestParam(value = "expectType", required = false) String expectType) {
		return ApiResult.ok(this.tenantIdentityService.currentIdentity(expectType));
	}

}
