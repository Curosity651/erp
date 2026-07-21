package com.erp.admin.wms.controller;

import java.util.List;

import com.erp.admin.wms.facade.FboWarehouseFacade;
import com.erp.admin.wms.facade.WarehouseManageFacade;
import com.erp.admin.wms.model.dto.WarehouseDTO;
import com.erp.admin.wms.model.qo.WarehouseQO;
import com.erp.admin.wms.model.vo.WarehouseOptionVO;
import com.erp.admin.wms.model.vo.WarehousePageVO;
import com.erp.admin.wms.service.WarehouseService;
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
 * 仓库管理
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/warehouse")
@Tag(name = "仓库管理")
public class WarehouseController {

	private final WarehouseManageFacade warehouseManageFacade;
	private final WarehouseService warehouseService;

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param warehouseQO 仓库查询对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('wms:warehouse:read')")
	public ApiResult<PageResult<WarehousePageVO>> getWarehousePage(PageParam pageParam, WarehouseQO warehouseQO) {
		return ApiResult.ok(this.warehouseManageFacade.queryPage(pageParam, warehouseQO));
	}

	/**
	 * 新增仓库
	 * @param warehouseDTO 仓库数据传输对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "新增仓库")
	@OperationLog(bizType = "仓库管理", successMessage = "新增成功")
	@PostMapping
	@PreAuthorize("@per.hasPermission('wms:warehouse:add')")
	public ApiResult<Void> save(@Validated @RequestBody WarehouseDTO warehouseDTO) {
		return this.warehouseManageFacade.createWarehouse(warehouseDTO) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "新增仓库失败");
	}

	/**
	 * 修改仓库
	 * @param warehouseDTO 仓库数据传输对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "修改仓库")
	@OperationLog(bizType = "仓库管理", successMessage = "修改成功")
	@PutMapping
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Void> updateById(@Validated @RequestBody WarehouseDTO warehouseDTO) {
		return this.warehouseManageFacade.updateWarehouse(warehouseDTO) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "修改仓库失败");
	}

	/**
	 * 获取仓库下拉选项列表
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取仓库下拉选项列表")
	@GetMapping("/options")
	@PreAuthorize("@per.hasPermission('wms:warehouse:read')")
	public ApiResult<List<WarehouseOptionVO>> getWarehouseOptions() {
		return ApiResult.ok(this.warehouseService.getWarehouseOptions());
	}

	/**
	 * 获取仓库详情
	 * @param id 仓库ID
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取仓库详情")
	@GetMapping("/detail")
	@PreAuthorize("@per.hasPermission('wms:warehouse:read')")
	public ApiResult<WarehousePageVO> getDetail(@RequestParam Long id) {
		return ApiResult.ok(this.warehouseService.getDetail(id));
	}

	/**
	 * 更新仓库状态
	 * @param warehouseStatusDTO 状态请求体
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "更新仓库状态")
	@OperationLog(bizType = "仓库管理", successMessage = "状态更新成功")
	@PatchMapping("/status")
	@PreAuthorize("@per.hasPermission('wms:warehouse:edit')")
	public ApiResult<Void> updateStatus(@RequestBody WarehouseStatusDTO warehouseStatusDTO) {
		try {
			this.warehouseService.updateStatus(warehouseStatusDTO.getId(), warehouseStatusDTO.getStatus());
			return ApiResult.ok();
		}
		catch (IllegalArgumentException e) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, e.getMessage());
		}
		catch (Exception e) {
			return ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "更新仓库状态失败");
		}
	}

	@lombok.Data
	public static class WarehouseStatusDTO {

		private Long id;

		private Integer status;

	}

}