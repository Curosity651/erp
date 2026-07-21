package com.erp.admin.statistics.controller;

import javax.validation.Valid;

import com.erp.admin.statistics.model.SalesTarget;
import com.erp.admin.statistics.model.dto.MonthlyTargetDTO;
import com.erp.admin.statistics.model.dto.SalesTargetBatchDTO;
import com.erp.admin.statistics.model.dto.SalesTargetBatchMonthlyUpdateDTO;
import com.erp.admin.statistics.model.dto.SalesTargetUpdateDTO;
import com.erp.admin.statistics.model.vo.SalesTargetYearlyOverviewVO;
import com.erp.admin.statistics.service.SalesTargetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.common.model.result.BaseResultCode;
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
 * 销售目标
 *
 * @author erp 2025-10-25 21:40:57
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/sales-target")
@Tag(name = "销售目标管理")
public class SalesTargetController {

	private final SalesTargetService salesTargetService;

	/**
	 * 新增销售目标
	 *
	 * @param salesTarget 销售目标
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "新增销售目标")
	@PostMapping
	@PreAuthorize("@per.hasPermission('system:sales-target:add')")
	public ApiResult<Void> save(@RequestBody SalesTarget salesTarget) {
		return salesTargetService.save(salesTarget) ?
				ApiResult.ok() : ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "新增销售目标失败");
	}

	/**
	 * 修改销售目标
	 *
	 * @param salesTarget 销售目标
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "修改销售目标")
	@PutMapping
	@PreAuthorize("@per.hasPermission('system:sales-target:edit')")
	public ApiResult<Void> updateById(@RequestBody SalesTarget salesTarget) {
		return salesTargetService.updateById(salesTarget) ?
				ApiResult.ok() : ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "修改销售目标失败");
	}

	/**
	 * 通过id删除销售目标
	 *
	 * @param id id
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "通过id删除销售目标")
	@DeleteMapping("/{id}")
	@PreAuthorize("@per.hasPermission('system:sales-target:del')")
	public ApiResult<Void> removeById(@PathVariable("id") Long id) {
		return salesTargetService.removeById(id) ?
				ApiResult.ok() : ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR, "通过id删除销售目标失败");
	}

	/**
	 * 批量创建年度和月度目标
	 *
	 * @param dto 批量创建DTO
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "批量创建年度和月度目标")
	@PostMapping("/batch")
	@PreAuthorize("@per.hasPermission('system:sales-target:add')")
	public ApiResult<Void> batchCreate(@Valid @RequestBody SalesTargetBatchDTO dto) {
		salesTargetService.saveYearlyWithMonthlyTargets(dto);
		return ApiResult.ok();
	}

	/**
	 * 获取年度概览
	 *
	 * @param year 目标年份
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取年度概览")
	@GetMapping("/yearly/{year}")
	@PreAuthorize("@per.hasPermission('system:sales-target:read')")
	public ApiResult<SalesTargetYearlyOverviewVO> getYearlyOverview(@PathVariable("year") Integer year) {
		return ApiResult.ok(salesTargetService.getYearlyOverview(year));
	}

	/**
	 * 批量更新月度目标
	 *
	 * @param dto 批量更新DTO
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "批量更新月度目标")
	@PutMapping("/batch-monthly")
	@PreAuthorize("@per.hasPermission('system:sales-target:edit')")
	public ApiResult<Void> batchUpdateMonthly(@Valid @RequestBody SalesTargetBatchMonthlyUpdateDTO dto) {
		salesTargetService.batchUpdateMonthlyTargets(dto);
		return ApiResult.ok();
	}

	/**
	 * 更新单个月度目标
	 *
	 * @param id  目标ID
	 * @param dto 更新DTO
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "更新单个月度目标")
	@PutMapping("/monthly/{id}")
	@PreAuthorize("@per.hasPermission('system:sales-target:edit')")
	public ApiResult<Void> updateMonthly(@PathVariable("id") Long id, @Valid @RequestBody SalesTargetUpdateDTO dto) {
		dto.setId(id);
		salesTargetService.updateTarget(dto);
		return ApiResult.ok();
	}

	/**
	 * 创建单个月度目标
	 *
	 * @param dto 月度目标DTO
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "创建单个月度目标")
	@PostMapping("/monthly")
	@PreAuthorize("@per.hasPermission('system:sales-target:add')")
	public ApiResult<Void> createMonthly(@Valid @RequestBody MonthlyTargetDTO dto) {
		// 暂时从dto中获取,需要扩展DTO
		salesTargetService.createSingleMonthlyTarget(dto);
		return ApiResult.ok();
	}

	/**
	 * 创建年度目标
	 *
	 * @param dto 年度目标创建DTO
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "创建年度目标")
	@PostMapping("/annual")
	@PreAuthorize("@per.hasPermission('system:sales-target:add')")
	public ApiResult<Void> createAnnual(@Valid @RequestBody com.erp.admin.statistics.model.dto.AnnualTargetCreateDTO dto) {
		salesTargetService.createAnnualTarget(dto);
		return ApiResult.ok();
	}

	/**
	 * 更新年度目标
	 *
	 * @param id  目标ID
	 * @param dto 更新DTO
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "更新年度目标")
	@PutMapping("/annual/{id}")
	@PreAuthorize("@per.hasPermission('system:sales-target:edit')")
	public ApiResult<Void> updateAnnual(@PathVariable("id") Long id, @Valid @RequestBody SalesTargetUpdateDTO dto) {
		dto.setId(id);
		salesTargetService.updateTarget(dto);
		return ApiResult.ok();
	}

}