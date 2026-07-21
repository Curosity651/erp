package com.erp.admin.system.controller;

import com.erp.admin.system.model.entity.Position;
import com.erp.admin.system.model.qo.PositionQO;
import com.erp.admin.system.model.vo.PositionPageVO;
import com.erp.admin.system.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.common.model.result.BaseResultCode;
import org.ballcat.log.operation.annotation.OperationLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 岗位管理表
 *
 * @author ballcat 2025-07-26 15:27:26
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/position")
@Tag(name = "岗位管理表管理")
public class PositionController {

	private final PositionService positionService;

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param positionQO 岗位管理表查询对象
	 * @return R 通用返回体
	 */
	@Operation(summary = "分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('system:position:read')")
	public ApiResult<PageResult<PositionPageVO>> getPositionPage(PageParam pageParam, PositionQO positionQO) {
		return ApiResult.ok(this.positionService.queryPage(pageParam, positionQO));
	}

	/**
	 * 新增岗位管理表
	 * @param position 岗位管理表
	 * @return R 通用返回体
	 */
	@Operation(summary = "新增岗位管理表")
	@OperationLog(bizType = "岗位管理表", successMessage = "新增成功")
	@PostMapping
	@PreAuthorize("@per.hasPermission('system:position:add')")
	public ApiResult<Void> save(@RequestBody Position position) {
		return this.positionService.save(position) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "新增岗位管理表失败");
	}

	/**
	 * 修改岗位管理表
	 * @param position 岗位管理表
	 * @return R 通用返回体
	 */
	@Operation(summary = "修改岗位管理表")
	@OperationLog(bizType = "岗位管理表", successMessage = "修改成功")
	@PutMapping
	@PreAuthorize("@per.hasPermission('system:position:edit')")
	public ApiResult<Void> updateById(@RequestBody Position position) {
		return this.positionService.updateById(position) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "修改岗位管理表失败");
	}

	/**
	 * 通过id删除岗位管理表
	 * @param id id
	 * @return R 通用返回体
	 */
	@Operation(summary = "通过id删除岗位管理表")
	@OperationLog(bizType = "岗位管理表", successMessage = "删除成功")
	@DeleteMapping("/{id}")
	@PreAuthorize("@per.hasPermission('system:position:del')")
	public ApiResult<Void> removeById(@PathVariable("id") Long id) {
		return this.positionService.removeById(id) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "通过id删除岗位管理表失败");
	}

}
