package com.erp.admin.system.controller;

import java.util.List;

import com.erp.admin.system.model.entity.ProjectGroup;
import com.erp.admin.system.model.qo.ProjectGroupQO;
import com.erp.admin.system.model.vo.ProjectGroupListVO;
import com.erp.admin.system.model.vo.ProjectGroupPageVO;
import com.erp.admin.system.service.ProjectGroupService;
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
 * 项目组管理表
 *
 * @author ballcat 2025-07-26 15:27:25
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/project-group")
@Tag(name = "项目组管理表管理")
public class ProjectGroupController {

	private final ProjectGroupService projectGroupService;

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param projectGroupQO 项目组管理表查询对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('system:project-group:read')")
	public ApiResult<PageResult<ProjectGroupPageVO>> getProjectGroupPage(PageParam pageParam,
			ProjectGroupQO projectGroupQO) {
		return ApiResult.ok(this.projectGroupService.queryPage(pageParam, projectGroupQO));
	}

	/**
	 * 新增项目组管理表
	 * @param projectGroup 项目组管理表
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "新增项目组管理表")
	@OperationLog(bizType = "项目组管理表", successMessage = "新增成功")
	@PostMapping
	@PreAuthorize("@per.hasPermission('system:project-group:add')")
	public ApiResult<Void> save(@RequestBody ProjectGroup projectGroup) {
		return this.projectGroupService.save(projectGroup) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "新增项目组管理表失败");
	}

	/**
	 * 修改项目组管理表
	 * @param projectGroup 项目组管理表
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "修改项目组管理表")
	@OperationLog(bizType = "项目组管理表", successMessage = "修改成功")
	@PutMapping
	@PreAuthorize("@per.hasPermission('system:project-group:edit')")
	public ApiResult<Void> updateById(@RequestBody ProjectGroup projectGroup) {
		return this.projectGroupService.updateById(projectGroup) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "修改项目组管理表失败");
	}

	/**
	 * 获取项目组列表（用于下拉选择）
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取项目组列表")
	@GetMapping("/list")
	@PreAuthorize("@per.hasPermission('system:project-group:read')")
	public ApiResult<List<ProjectGroupListVO>> getProjectGroupList() {
		return ApiResult.ok(this.projectGroupService.getProjectGroupList());
	}

	/**
	 * 通过id删除项目组管理表
	 * @param id id
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "通过id删除项目组管理表")
	@OperationLog(bizType = "项目组管理表", successMessage = "删除成功")
	@DeleteMapping("/{id}")
	@PreAuthorize("@per.hasPermission('system:project-group:del')")
	public ApiResult<Void> removeById(@PathVariable("id") Long id) {
		return this.projectGroupService.removeById(id) ? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "通过id删除项目组管理表失败");
	}

}
