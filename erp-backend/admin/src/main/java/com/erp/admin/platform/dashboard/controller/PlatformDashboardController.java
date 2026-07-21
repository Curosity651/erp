package com.erp.admin.platform.dashboard.controller;

import com.erp.admin.platform.dashboard.model.qo.PlatformDashboardQO;
import com.erp.admin.platform.dashboard.model.vo.PlatformDashboardDataVO;
import com.erp.admin.platform.dashboard.service.PlatformDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 海外仓平台数据分析（平台数据分析 900300）。跨全部货主的仓储运营聚合看板。
 * 身份校验在 service 内（仅 OVERSEAS_PLATFORM）。
 *
 * @author erp
 */
@Tag(name = "海外仓平台数据分析")
@RestController
@RequestMapping("/api/platform-dashboard")
@RequiredArgsConstructor
public class PlatformDashboardController {

    private final PlatformDashboardService platformDashboardService;

    @Operation(summary = "平台数据分析看板聚合数据")
    @PostMapping("/data")
    @PreAuthorize("@per.hasPermission('platform:dashboard:read')")
    public ApiResult<PlatformDashboardDataVO> data(@RequestBody PlatformDashboardQO qo) {
        return ApiResult.ok(this.platformDashboardService.getData(qo));
    }

}
