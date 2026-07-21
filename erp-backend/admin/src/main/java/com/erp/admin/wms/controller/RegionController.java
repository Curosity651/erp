package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.dto.PlatformRegionMappingDTO;
import com.erp.admin.wms.model.dto.RegionDTO;
import com.erp.admin.wms.model.qo.RegionQO;
import com.erp.admin.wms.model.vo.PlatformRegionMappingVO;
import com.erp.admin.wms.model.vo.RegionOptionVO;
import com.erp.admin.wms.model.vo.RegionPageVO;
import com.erp.admin.wms.facade.RegionManageFacade;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 区域管理
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/region")
@Tag(name = "区域管理")
public class RegionController {

	private final RegionManageFacade regionManageFacade;

	@Operation(summary = "区域分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('wms:region:read')")
	public ApiResult<PageResult<RegionPageVO>> getRegionPage(PageParam pageParam, RegionQO qo) {
		return ApiResult.ok(this.regionManageFacade.queryPage(pageParam, qo));
	}

	@Operation(summary = "区域下拉列表")
	@GetMapping("/list")
	@PreAuthorize("@per.hasPermission('wms:region:read')")
	public ApiResult<List<RegionOptionVO>> getRegionOptions() {
		return ApiResult.ok(this.regionManageFacade.getRegionOptions());
	}

	@Operation(summary = "新增区域")
	@OperationLog(bizType = "区域管理", successMessage = "新增成功")
	@PostMapping
	@PreAuthorize("@per.hasPermission('wms:region:add')")
	public ApiResult<Void> save(@Validated @RequestBody RegionDTO dto) {
		return this.regionManageFacade.createRegion(dto)
				? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "新增区域失败");
	}

	@Operation(summary = "编辑区域")
	@OperationLog(bizType = "区域管理", successMessage = "编辑成功")
	@PutMapping
	@PreAuthorize("@per.hasPermission('wms:region:edit')")
	public ApiResult<Void> update(@Validated @RequestBody RegionDTO dto) {
		return this.regionManageFacade.updateRegion(dto)
				? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "编辑区域失败");
	}

	@Operation(summary = "删除区域")
	@OperationLog(bizType = "区域管理", successMessage = "删除成功")
	@DeleteMapping
	@PreAuthorize("@per.hasPermission('wms:region:del')")
	public ApiResult<Void> delete(@RequestParam Long id) {
		return this.regionManageFacade.deleteRegion(id)
				? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "删除区域失败");
	}

	// ========== 平台映射 ==========

	@Operation(summary = "查询所有平台映射")
	@GetMapping("/platform-mappings/all")
	@PreAuthorize("@per.hasPermission('wms:region:read')")
	public ApiResult<List<PlatformRegionMappingVO>> getAllPlatformMappings() {
		return ApiResult.ok(this.regionManageFacade.getAllPlatformMappings());
	}

	@Operation(summary = "保存平台映射")
	@OperationLog(bizType = "区域管理", successMessage = "保存平台映射成功")
	@PostMapping("/platform-mappings/save")
	@PreAuthorize("@per.hasPermission('wms:region:edit')")
	public ApiResult<Void> savePlatformMapping(@Validated @RequestBody PlatformRegionMappingDTO dto) {
		return this.regionManageFacade.savePlatformMapping(dto.getPlatform(), dto.getRegionId())
				? ApiResult.ok()
				: ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "保存平台映射失败");
	}

}
