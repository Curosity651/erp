package com.erp.admin.wms.controller;

import com.erp.admin.platform.finance.model.qo.MonthlyBillQO;
import com.erp.admin.platform.finance.model.vo.MonthlyBillVO;
import com.erp.admin.wms.model.vo.OperatorIncomeVO;
import com.erp.admin.wms.service.OperatorFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * WMS 服务商财务控制器：收入（物流产品使用计费聚合）+ 支出（平台月度账单只读对账）。
 * 服务商身份在 service 内校验。
 *
 * @author erp
 */
@Tag(name = "服务商财务(收入/支出)")
@RestController
@RequestMapping("/wms/operator-finance")
@RequiredArgsConstructor
@PreAuthorize("@per.hasPermission('wms:operator-finance:read')")
public class OperatorFinanceController {

    private final OperatorFinanceService operatorFinanceService;

    @Operation(summary = "收入汇总(按产品×月: 次数×单价)")
    @GetMapping("/income/summary")
    public ApiResult<OperatorIncomeVO.Summary> incomeSummary(@RequestParam(required = false) String monthStart,
            @RequestParam(required = false) String monthEnd, @RequestParam(required = false) Long erpTenantId) {
        return ApiResult.ok(operatorFinanceService.incomeSummary(monthStart, monthEnd, erpTenantId));
    }

    @Operation(summary = "收入明细流水")
    @GetMapping("/income/records")
    public ApiResult<List<OperatorIncomeVO.Record>> incomeRecords(@RequestParam(required = false) String monthStart,
            @RequestParam(required = false) String monthEnd, @RequestParam(required = false) Long erpTenantId,
            @RequestParam(required = false) Long productId) {
        return ApiResult.ok(operatorFinanceService.incomeRecords(monthStart, monthEnd, erpTenantId, productId));
    }

    @Operation(summary = "支出账单分页(只读对账)")
    @GetMapping("/expense/page")
    public ApiResult<PageResult<MonthlyBillVO>> expensePage(PageParam pageParam, MonthlyBillQO qo) {
        return ApiResult.ok(operatorFinanceService.expensePage(pageParam, qo));
    }

    @Operation(summary = "支出账单详情")
    @GetMapping("/expense/{id}")
    public ApiResult<MonthlyBillVO> expenseDetail(@PathVariable("id") Long id) {
        return ApiResult.ok(operatorFinanceService.expenseDetail(id));
    }

}
