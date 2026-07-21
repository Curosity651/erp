package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.vo.OperatorDashboardVO;
import com.erp.admin.wms.service.OperatorDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * WMS 服务商运营数据分析看板控制器（900400）。服务商身份在 service 内校验。
 *
 * @author erp
 */
@Tag(name = "服务商运营数据分析")
@RestController
@RequestMapping("/wms/operator-dashboard")
@RequiredArgsConstructor
@PreAuthorize("@per.hasPermission('wms:operator-dashboard:read')")
public class OperatorDashboardController {

    private final OperatorDashboardService operatorDashboardService;

    @Operation(summary = "运营看板聚合数据(月区间默认最近6个月)")
    @GetMapping("/data")
    public ApiResult<OperatorDashboardVO> data(@RequestParam(required = false) String monthStart,
            @RequestParam(required = false) String monthEnd,
            @RequestParam(required = false) List<Long> erpTenantIds) {
        return ApiResult.ok(operatorDashboardService.getData(monthStart, monthEnd, erpTenantIds));
    }

}
