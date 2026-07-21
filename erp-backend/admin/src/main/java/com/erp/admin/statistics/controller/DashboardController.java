package com.erp.admin.statistics.controller;

import java.util.List;

import com.erp.admin.statistics.model.dto.DashboardQueryDTO;
import com.erp.admin.statistics.model.qo.SkuRankingQO;
import com.erp.admin.statistics.model.vo.DashboardDataVO;
import com.erp.admin.statistics.model.vo.SkuRankingExportVO;
import com.erp.admin.statistics.model.vo.SkuRankingItemVO;
import com.erp.admin.statistics.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.common.model.result.BaseResultCode;
import org.ballcat.fastexcel.annotation.ResponseExcel;
import org.ballcat.web.accesslog.annotation.AccessLoggingRule;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard统计数据控制器
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard统计")
public class DashboardController {

	private final DashboardService dashboardService;

	/**
	 * 获取Dashboard统计数据
	 *
	 * @param queryDTO 查询参数
	 * @return Dashboard数据
	 */
	@Operation(summary = "获取Dashboard数据")
	@PostMapping("/data")
	@PreAuthorize("@per.hasPermission('statistics:dashboard:read')")
	public ApiResult<DashboardDataVO> getDashboardData(@RequestBody DashboardQueryDTO queryDTO) {
		try {
			DashboardDataVO data = dashboardService.getDashboardData(queryDTO);
			return ApiResult.ok(data);
		}
		catch (IllegalArgumentException e) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, e.getMessage());
		}
		catch (Exception e) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "获取Dashboard数据失败: " + e.getMessage());
		}
	}

	/**
	 * 分页查询SKU销量排名
	 *
	 * @param pageParam 分页参数
	 * @param qo 查询条件
	 * @return 分页结果
	 */
	@Operation(summary = "分页查询SKU销量排名")
	@PostMapping("/sku-ranking/page")
	@PreAuthorize("@per.hasPermission('statistics:dashboard:read')")
	public ApiResult<PageResult<SkuRankingItemVO>> getSkuRankingPage(@ParameterObject PageParam pageParam,
			@RequestBody SkuRankingQO qo) {
		try {
			PageResult<SkuRankingItemVO> result = dashboardService.getSkuRankingPage(pageParam, qo);
			return ApiResult.ok(result);
		}
		catch (IllegalArgumentException e) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, e.getMessage());
		}
		catch (Exception e) {
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR, "获取SKU排名失败: " + e.getMessage());
		}
	}

	/**
	 * 导出SKU销量排名
	 *
	 * @param qo 查询条件
	 * @return Excel文件
	 */
	@Operation(summary = "导出SKU销量排名")
	@PostMapping("/sku-ranking/export")
	@AccessLoggingRule(includeQueryString = true, includeRequestBody = true)
	@PreAuthorize("@per.hasPermission('statistics:dashboard:export')")
	@ResponseExcel(name = "SKU销量排名_#{currentDateTime()}")
	public List<SkuRankingExportVO> exportSkuRanking(@RequestBody SkuRankingQO qo) {
		return dashboardService.getSkuRankingExportData(qo);
	}

}
