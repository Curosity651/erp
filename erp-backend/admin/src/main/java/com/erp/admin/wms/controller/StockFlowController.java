package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.qo.StockFlowQO;
import com.erp.admin.wms.model.vo.StockFlowDetailVO;
import com.erp.admin.wms.model.vo.StockFlowPageVO;
import com.erp.admin.wms.model.vo.StockFlowTodaySummaryVO;
import com.erp.admin.wms.model.vo.StockFlowTrendVO;
import com.erp.admin.wms.service.InventoryEventQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 库存流水控制器
 *
 * @author erp
 */
@Tag(name = "库存流水")
@RestController
@RequestMapping("/wms/stock-flow")
@RequiredArgsConstructor
public class StockFlowController {

    private final InventoryEventQueryService inventoryEventQueryService;

    @Operation(summary = "流水分页查询")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('wms:stock-flow:read')")
    public ApiResult<PageResult<StockFlowPageVO>> queryPage(PageParam pageParam, StockFlowQO qo) {
        return ApiResult.ok(inventoryEventQueryService.queryFlowPage(pageParam, qo));
    }

    @Operation(summary = "流水详情")
    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('wms:stock-flow:read')")
    public ApiResult<StockFlowDetailVO> getDetail(@RequestParam Long id) {
        return ApiResult.ok(inventoryEventQueryService.getFlowDetail(id));
    }

    @Operation(summary = "该仓+SKU实际出现过的过账类型(用于筛选下拉动态选项)")
    @GetMapping("/posting-types")
    @PreAuthorize("hasAuthority('wms:stock-flow:read')")
    public ApiResult<List<String>> listPostingTypes(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String skuCode) {
        return ApiResult.ok(inventoryEventQueryService.listEventTypes(warehouseId, skuCode));
    }

    @Operation(summary = "今日汇总统计")
    @GetMapping("/today-summary")
    @PreAuthorize("hasAuthority('wms:stock-flow:read')")
    public ApiResult<StockFlowTodaySummaryVO> getTodaySummary(
            @RequestParam(required = false) Long warehouseId) {
        return ApiResult.ok(inventoryEventQueryService.getTodaySummary(warehouseId));
    }

    @Operation(summary = "流水趋势数据")
    @GetMapping("/trend")
    @PreAuthorize("hasAuthority('wms:stock-flow:read')")
    public ApiResult<List<StockFlowTrendVO>> getTrend(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(required = false) Long warehouseId) {
        return ApiResult.ok(inventoryEventQueryService.getTrend(days, warehouseId));
    }

}
