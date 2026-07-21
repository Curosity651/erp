package com.erp.admin.financial.controller;

import java.util.List;

import com.erp.admin.financial.model.qo.OrderReconciliationQO;
import com.erp.admin.financial.model.vo.OrderReconciliationDetailVO;
import com.erp.admin.financial.model.vo.OrderReconciliationExportVO;
import com.erp.admin.financial.model.vo.OrderReconciliationStatsVO;
import com.erp.admin.financial.model.vo.OrderReconciliationVO;
import com.erp.admin.financial.service.OrderReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.fastexcel.annotation.ResponseExcel;
import org.ballcat.web.accesslog.annotation.AccessLoggingRule;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * WB-订单财务对账 Controller
 * <p>
 * 提供订单财务对账相关接口，按设计方案分为两个 Tab：
 * - 订单财务对账：以订单为主体，展示订单的财务对账状态
 *
 * @author system
 */
@RestController
@RequestMapping("/financial/reconciliation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WB-订单财务对账")
public class OrderReconciliationController {

    private final OrderReconciliationService orderReconciliationService;

    // ==================== 订单财务对账相关接口 ====================

    /**
     * 获取订单对账统计数据
     *
     * @param qo 查询条件（店铺、报表类型、日期范围等）
     * @return 统计数据
     */
    @PreAuthorize("@per.hasPermission('financial:reconciliation:read')")
    @PostMapping("/orders/stats")
    @Operation(summary = "WB-获取订单对账统计", description = "获取订单财务对账统计数据，包括订单总数、已对账、待对账、异常数量")
    public ApiResult<OrderReconciliationStatsVO> getOrderStats(@Validated @RequestBody OrderReconciliationQO qo) {
        log.info("[API] 获取订单对账统计: periodType={}, shopIds={}, startDate={}, endDate={}",
                qo.getPeriodType(), qo.getShopIds(), qo.getStartDate(), qo.getEndDate());
        OrderReconciliationStatsVO stats = orderReconciliationService.getOrderStats(qo);
        return ApiResult.ok(stats);
    }

    /**
     * 分页查询订单财务对账列表
     *
     * @param pageParam 分页参数
     * @param qo        查询条件
     * @return 分页结果
     */
    @PreAuthorize("@per.hasPermission('financial:reconciliation:read')")
    @PostMapping("/orders")
    @Operation(summary = "WB-分页查询订单对账列表", description = "分页查询订单财务对账列表，支持按对账状态筛选")
    public ApiResult<PageResult<OrderReconciliationVO>> pageOrders(
            PageParam pageParam,
            @RequestBody @Validated OrderReconciliationQO qo) {
        log.info("[API] 分页查询订单对账: page={}, size={}, periodType={}, status={}",
                pageParam.getPage(), pageParam.getSize(), qo.getPeriodType(), qo.getReconciliationStatus());
        PageResult<OrderReconciliationVO> result = orderReconciliationService.pageOrderReconciliation(pageParam, qo);
        log.info("[API] 查询成功: total={}", result.getTotal());
        return ApiResult.ok(result);
    }

    /**
     * 获取订单财务对账详情
     *
     * @param orderId    订单ID
     * @param periodType 报表类型
     * @return 订单对账详情（包含关联财务记录）
     */
    @PreAuthorize("@per.hasPermission('financial:reconciliation:read')")
    @GetMapping("/orders/detail")
    @Operation(summary = "WB-获取订单对账详情", description = "获取指定订单的财务对账详情，包括订单信息和关联的财务记录")
    public ApiResult<OrderReconciliationDetailVO> getOrderDetail(
            @Parameter(description = "订单ID", required = true)
            @RequestParam("orderId") Long orderId,
            @Parameter(description = "报表类型：weekly-周报, daily-日报", required = true)
            @RequestParam("periodType") String periodType) {
        log.info("[API] 获取订单对账详情: orderId={}, periodType={}", orderId, periodType);
        OrderReconciliationDetailVO detail = orderReconciliationService.getOrderReconciliationDetail(orderId, periodType);
        if (detail == null) {
            return ApiResult.failed(404, "订单不存在");
        }
        return ApiResult.ok(detail);
    }

    /**
     * 导出订单对账数据
     *
     * @param qo 查询条件
     * @return Excel 文件数据
     */
    @PreAuthorize("@per.hasPermission('financial:reconciliation:export')")
    @PostMapping("/orders/export")
	@AccessLoggingRule(includeQueryString = true, includeRequestBody = true)
	@Operation(summary = "WB-导出订单对账数据", description = "根据查询条件导出订单对账数据为 Excel 文件")
    @ResponseExcel(name = "WB-订单财务对账_#{currentDateTime()}")
    public List<OrderReconciliationExportVO> exportOrders(@RequestBody @Validated OrderReconciliationQO qo) {
        log.info("[API] 导出订单对账: periodType={}, shopIds={}", qo.getPeriodType(), qo.getShopIds());
        List<OrderReconciliationExportVO> list = orderReconciliationService.exportOrderReconciliation(qo);
        log.info("[API] 导出数据量: {}", list.size());
        return list;
    }

}
