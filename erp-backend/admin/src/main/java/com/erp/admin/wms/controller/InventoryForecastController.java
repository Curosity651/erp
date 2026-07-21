package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.qo.ForecastDetailQO;
import com.erp.admin.wms.model.qo.ForecastSummaryQO;
import com.erp.admin.wms.model.vo.ForecastDetailVO;
import com.erp.admin.wms.model.vo.ForecastSummaryResult;
import com.erp.admin.wms.model.vo.IncomingPlanVO;
import com.erp.admin.wms.facade.InventoryForecastFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 库存预测 Controller
 */
@Tag(name = "库存预测", description = "区域维度的库存预测")
@RestController
@RequestMapping("/wms/inventory-forecast")
@RequiredArgsConstructor
public class InventoryForecastController {

    private final InventoryForecastFacade inventoryForecastFacade;

    @Operation(summary = "获取库存预测汇总")
    @GetMapping("/summary")
    @PreAuthorize("@per.hasPermission('wms:inventory-forecast:read')")
    public ApiResult<ForecastSummaryResult> getForecastSummary(PageParam pageParam, ForecastSummaryQO qo) {
        return ApiResult.ok(inventoryForecastFacade.getSummary(pageParam, qo));
    }

    @Operation(summary = "获取预测详情")
    @GetMapping("/detail")
    @PreAuthorize("@per.hasPermission('wms:inventory-forecast:read')")
    public ApiResult<ForecastDetailVO> getForecastDetail(@Valid ForecastDetailQO qo) {
        return ApiResult.ok(inventoryForecastFacade.getDetail(qo));
    }

    @Operation(summary = "获取入库计划（仓库级）")
    @GetMapping("/incoming")
    @PreAuthorize("@per.hasPermission('wms:inventory-forecast:read')")
    public ApiResult<List<IncomingPlanVO>> getIncomingPlan(
            @RequestParam Long warehouseId,
            @RequestParam String skuCode) {
        return ApiResult.ok(inventoryForecastFacade.getIncomingPlan(warehouseId, skuCode));
    }
}
