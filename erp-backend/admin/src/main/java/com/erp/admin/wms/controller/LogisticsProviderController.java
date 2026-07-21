package com.erp.admin.wms.controller;

import java.util.List;

import com.erp.admin.wms.model.dto.LogisticsProviderDTO;
import com.erp.admin.wms.model.qo.LogisticsProviderQO;
import com.erp.admin.wms.model.vo.LogisticsProviderOptionVO;
import com.erp.admin.wms.model.vo.LogisticsProviderPageVO;
import com.erp.admin.wms.service.LogisticsProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.common.model.result.BaseResultCode;
import org.ballcat.log.operation.annotation.OperationLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物流商管理
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/logistics-provider")
@Tag(name = "物流商管理")
public class LogisticsProviderController {

	private final LogisticsProviderService logisticsProviderService;

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 物流商查询对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('wms:logistics-provider:read')")
	public ApiResult<PageResult<LogisticsProviderPageVO>> getPage(PageParam pageParam, LogisticsProviderQO qo) {
		return ApiResult.ok(this.logisticsProviderService.queryPage(pageParam, qo));
	}

	/**
	 * 新增物流商
	 * @param dto 物流商数据传输对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "新增物流商")
	@OperationLog(bizType = "物流商管理", successMessage = "新增成功")
	@PostMapping
	@PreAuthorize("@per.hasPermission('wms:logistics-provider:add')")
	public ApiResult<Void> save(@Validated @RequestBody LogisticsProviderDTO dto) {
		return this.logisticsProviderService.createProvider(dto) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "新增物流商失败");
	}

	/**
	 * 修改物流商
	 * @param dto 物流商数据传输对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "修改物流商")
	@OperationLog(bizType = "物流商管理", successMessage = "修改成功")
	@PutMapping
	@PreAuthorize("@per.hasPermission('wms:logistics-provider:edit')")
	public ApiResult<Void> updateById(@Validated @RequestBody LogisticsProviderDTO dto) {
		return this.logisticsProviderService.updateProvider(dto) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "修改物流商失败");
	}

	/**
	 * 获取物流商下拉选项列表
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取物流商下拉选项列表")
	@GetMapping("/options")
	@PreAuthorize("@per.hasPermission('wms:logistics-provider:read')")
	public ApiResult<List<LogisticsProviderOptionVO>> getOptions() {
		return ApiResult.ok(this.logisticsProviderService.getProviderOptions());
	}

	/**
	 * 获取物流商详情
	 * @param id 物流商ID
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取物流商详情")
	@GetMapping("/detail")
	@PreAuthorize("@per.hasPermission('wms:logistics-provider:read')")
	public ApiResult<LogisticsProviderPageVO> getDetail(@RequestParam Long id) {
		return ApiResult.ok(this.logisticsProviderService.getDetail(id));
	}

	/**
	 * 更新物流商状态
	 * @param statusBody 状态请求体
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "更新物流商状态")
	@OperationLog(bizType = "物流商管理", successMessage = "状态更新成功")
	@PatchMapping("/status")
	@PreAuthorize("@per.hasPermission('wms:logistics-provider:edit')")
	public ApiResult<Void> updateStatus(@RequestBody StatusBody statusBody) {
		try {
			this.logisticsProviderService.updateStatus(statusBody.getId(), statusBody.getStatus());
			return ApiResult.ok();
		}
		catch (IllegalArgumentException e) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, e.getMessage());
		}
		catch (Exception e) {
			return ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "更新物流商状态失败");
		}
	}

	@lombok.Data
	public static class StatusBody {

		private Long id;

		private Integer status;

	}

}
