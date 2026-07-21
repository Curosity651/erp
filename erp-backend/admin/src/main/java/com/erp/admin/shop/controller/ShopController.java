package com.erp.admin.shop.controller;
import java.util.Objects;

import com.erp.admin.shop.facade.ShopFacade;
import com.erp.admin.shop.model.dto.CreateOrUpdateShopRequest;
import com.erp.admin.shop.model.dto.TestCredentialRequest;
import com.erp.admin.shop.model.dto.TestCredentialResponse;
import com.erp.admin.shop.model.enums.ShopStatusEnum;
import com.erp.admin.shop.model.qo.ShopQO;
import com.erp.admin.shop.model.vo.ShopDetailVO;
import com.erp.admin.shop.model.vo.ShopPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.common.model.result.BaseResultCode;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/shops")
@Tag(name = "店铺管理")
public class ShopController {

	private final ShopFacade shopFacade;

	@Operation(summary = "分页列表")
	@GetMapping
	@PreAuthorize("@per.hasPermission('system:shop:read')")
	public ApiResult<PageResult<ShopPageVO>> page(PageParam pageParam, @ParameterObject ShopQO qo) {
		PageResult<ShopPageVO> page = shopFacade.queryPage(pageParam, qo);
		if (page != null && page.getRecords() != null) {
			for (ShopPageVO it : page.getRecords()) {
				if (it != null && it.getLastTestStatus() == null) {
					int status;
					if (it.getLastTestedAt() == null) {
						status = 0;
					} else if (Objects.equals(it.getStatus(), ShopStatusEnum.ABNORMAL.getCode())) {
						status = 2;
					} else {
						status = 1;
					}
					it.setLastTestStatus(status);
				}
			}
		}
		return ApiResult.ok(page);
	}

	@Operation(summary = "详情")
	@GetMapping("/{id}")
	@PreAuthorize("@per.hasPermission('system:shop:read')")
	public ApiResult<ShopDetailVO> detail(@PathVariable Long id) {
		try {
			return ApiResult.ok(shopFacade.detail(id));
		} catch (IllegalArgumentException e) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, e.getMessage());
		}
	}

	@Operation(summary = "测试凭证")
	@PostMapping("/test")
	@PreAuthorize("@per.hasPermission('system:shop:edit')")
	public ApiResult<TestCredentialResponse> test(@RequestBody TestCredentialRequest req) {
		try {
			return ApiResult.ok(shopFacade.testCredential(req));
		} catch (IllegalArgumentException e) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, e.getMessage());
		}
	}

	@Operation(summary = "重新测试店铺凭证")
	@PostMapping("/{id}/test")
	@PreAuthorize("@per.hasPermission('system:shop:edit')")
	public ApiResult<Void> retest(@PathVariable Long id) {
		try {
			shopFacade.retest(id);
			return ApiResult.ok();
		} catch (IllegalArgumentException e) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, e.getMessage());
		} catch (Exception e) {
			return ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, e.getMessage());
		}
	}

	@Operation(summary = "创建店铺")
	@PostMapping
	@PreAuthorize("@per.hasPermission('system:shop:add')")
	public ApiResult<IdResp> create(@RequestBody CreateOrUpdateShopRequest req) {
		try {
			Long id = shopFacade.create(req);
			IdResp resp = new IdResp();
			resp.setId(id);
			return ApiResult.ok(resp);
		} catch (IllegalArgumentException e) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, e.getMessage());
		} catch (Exception e) {
			return ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, e.getMessage());
		}
	}

	@Operation(summary = "更新店铺")
	@PutMapping("/{id}")
	@PreAuthorize("@per.hasPermission('system:shop:edit')")
	public ApiResult<Void> update(@PathVariable Long id, @RequestBody CreateOrUpdateShopRequest req) {
		try {
			shopFacade.update(id, req);
			return ApiResult.ok();
		} catch (IllegalArgumentException e) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, e.getMessage());
		} catch (Exception e) {
			return ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, e.getMessage());
		}
	}

	@Operation(summary = "启用/禁用")
	@PatchMapping("/{id}/status")
	@PreAuthorize("@per.hasPermission('system:shop:edit')")
	public ApiResult<Void> toggleStatus(@PathVariable Long id, @RequestBody StatusBody body) {
		try {
			shopFacade.toggleStatus(id, body.getStatus());
			return ApiResult.ok();
		} catch (IllegalArgumentException e) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, e.getMessage());
		} catch (Exception e) {
			return ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, e.getMessage());
		}
	}

	@lombok.Data
	public static class IdResp {

		private Long id;

	}

	@lombok.Data
	public static class StatusBody {

		private Integer status;

	}

}
