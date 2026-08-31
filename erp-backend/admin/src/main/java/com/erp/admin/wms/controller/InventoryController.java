package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.qo.InventoryQO;
import com.erp.admin.wms.model.vo.*;
import com.erp.admin.wms.service.OwnerInventoryQueryService;
import com.erp.admin.wms.service.RegionInventoryService;
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
 * 库存管理控制器
 *
 * @author erp
 */
@Tag(name = "库存管理")
@RestController
@RequestMapping("/wms/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final OwnerInventoryQueryService inventoryQueryService;
    private final RegionInventoryService regionInventoryService;

    @Operation(summary = "库存汇总统计")
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('wms:inventory:overview')")
    public ApiResult<InventorySummaryVO> getSummary() {
        return ApiResult.ok(inventoryQueryService.getSummary());
    }

    @Operation(summary = "按仓库汇总")
    @GetMapping("/summary-by-warehouse")
    @PreAuthorize("hasAuthority('wms:inventory:overview')")
    public ApiResult<List<WarehouseSummaryVO>> getSummaryByWarehouse(
            @RequestParam(required = false) String warehouseType) {
        return ApiResult.ok(inventoryQueryService.getSummaryByWarehouse(warehouseType));
    }

    @Operation(summary = "按SKU汇总分页")
    @GetMapping("/summary-by-sku/page")
    @PreAuthorize("hasAuthority('wms:inventory:overview')")
    public ApiResult<PageResult<SkuSummaryVO>> getSummaryBySku(
            PageParam pageParam,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stockStatus) {
        return ApiResult.ok(inventoryQueryService.getSummaryBySku(pageParam, keyword, stockStatus));
    }

    @Operation(summary = "库存明细分页")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('wms:inventory:read')")
    public ApiResult<PageResult<InventoryPageVO>> queryPage(PageParam pageParam, InventoryQO qo) {
        return ApiResult.ok(inventoryQueryService.queryPage(pageParam, qo));
    }

    @Operation(summary = "库存明细详情")
    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('wms:inventory:read')")
    public ApiResult<InventoryDetailVO> getDetail(@RequestParam Long warehouseId,
                                                   @RequestParam String skuCode) {
        return ApiResult.ok(inventoryQueryService.getDetail(warehouseId, skuCode));
    }

    @Operation(summary = "按区域汇总")
    @GetMapping("/summary-by-region")
    @PreAuthorize("hasAuthority('wms:inventory:overview')")
    public ApiResult<List<RegionSummaryVO>> getSummaryByRegion() {
        return ApiResult.ok(regionInventoryService.getRegionSummaryList());
    }

    @Operation(summary = "区域库存统计卡片")
    @GetMapping("/region-stats")
    @PreAuthorize("hasAuthority('wms:inventory:overview')")
    public ApiResult<RegionInventoryStatsVO> getRegionStats() {
        return ApiResult.ok(regionInventoryService.getRegionStats());
    }

}
