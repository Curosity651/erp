package com.erp.admin.wms.controller;

import com.erp.admin.wms.facade.OzonFboStockSyncFacade;
import com.erp.admin.wms.model.enums.FboSyncType;
import com.erp.admin.wms.model.qo.FboSyncLogQO;
import com.erp.admin.wms.model.vo.FboSyncLogDetailVO;
import com.erp.admin.wms.model.vo.FboSyncLogPageVO;
import com.erp.admin.wms.model.vo.FboSyncResultVO;
import com.erp.admin.wms.service.FboSyncLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * FBO 库存同步控制器
 *
 * @author erp
 */
@RestController
@RequestMapping("/wms/fbo")
@RequiredArgsConstructor
@Tag(name = "FBO 库存同步")
public class FboStockSyncController {

    private final OzonFboStockSyncFacade ozonFboStockSyncFacade;
    private final FboSyncLogService fboSyncLogService;

    @PostMapping("/sync")
    @Operation(summary = "手动触发 FBO 库存同步")
    @PreAuthorize("hasAuthority('wms:warehouse:fbo-sync')")
    public ApiResult<List<FboSyncResultVO>> sync(@RequestParam(required = false) Long shopId) {
        List<FboSyncResultVO> results;
        if (shopId != null) {
            FboSyncResultVO result = ozonFboStockSyncFacade.syncByShopId(shopId);
            results = Collections.singletonList(result);
        } else {
            results = ozonFboStockSyncFacade.syncAllShops(FboSyncType.MANUAL.getCode());
        }
        return ApiResult.ok(results);
    }

    @GetMapping("/sync-log")
    @Operation(summary = "查询 FBO 同步日志")
    @PreAuthorize("hasAuthority('wms:warehouse:fbo-sync')")
    public ApiResult<PageResult<FboSyncLogPageVO>> querySyncLog(PageParam pageParam, FboSyncLogQO qo) {
        PageResult<FboSyncLogPageVO> page = fboSyncLogService.queryPage(pageParam, qo);
        return ApiResult.ok(page);
    }

    @GetMapping("/sync-log/detail")
    @Operation(summary = "查询 FBO 同步日志详情")
    @PreAuthorize("hasAuthority('wms:warehouse:fbo-sync')")
    public ApiResult<FboSyncLogDetailVO> getSyncLogDetail(@RequestParam Long id) {
        FboSyncLogDetailVO detail = fboSyncLogService.getDetail(id);
        return ApiResult.ok(detail);
    }

}
