package com.erp.admin.finance.settlement.controller;

import com.erp.admin.finance.settlement.model.dto.ReviewRechargeDTO;
import com.erp.admin.finance.settlement.model.dto.ReverseRechargeDTO;
import com.erp.admin.finance.settlement.model.qo.FundSettlementQO;
import com.erp.admin.finance.settlement.model.qo.FundLedgerExportQO;
import com.erp.admin.finance.settlement.model.vo.FundAccountVO;
import com.erp.admin.finance.settlement.model.vo.FundLedgerVO;
import com.erp.admin.finance.settlement.model.vo.FundLedgerMonthVO;
import com.erp.admin.finance.settlement.model.vo.RechargeOrderVO;
import com.erp.admin.finance.settlement.service.FundAccountQueryService;
import com.erp.admin.finance.settlement.service.RechargeOrderService;
import com.erp.admin.finance.settlement.service.FundLedgerExportService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/fund-settlement/platform")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('platform-finance:settle')")
public class PlatformFundSettlementController {
    private final FundAccountQueryService queryService;
    private final RechargeOrderService rechargeService;
    private final FundLedgerExportService ledgerExportService;

    @GetMapping("/accounts")
    public ApiResult<List<FundAccountVO>> accounts(FundSettlementQO qo) {
        return ApiResult.ok(queryService.platformAccounts(qo));
    }

    @GetMapping("/recharges")
    public ApiResult<List<RechargeOrderVO>> recharges(FundSettlementQO qo) {
        return ApiResult.ok(queryService.platformRecharges(qo));
    }

    @GetMapping("/ledger")
    public ApiResult<List<FundLedgerVO>> ledger(@RequestParam Long wmsTenantId, FundSettlementQO qo) {
        return ApiResult.ok(queryService.platformLedger(wmsTenantId, qo));
    }

    @GetMapping("/ledger/months")
    public ApiResult<List<FundLedgerMonthVO>> ledgerMonths(@RequestParam Long wmsTenantId, FundSettlementQO qo) {
        return ApiResult.ok(queryService.platformLedgerMonths(wmsTenantId, qo));
    }

    @GetMapping("/ledger/export")
    public ResponseEntity<byte[]> exportLedger(@Valid FundLedgerExportQO exportQO) {
        if (exportQO.getStartDate().isAfter(exportQO.getEndDate())) {
            throw new org.ballcat.common.core.exception.BusinessException(400, "开始日期不能晚于结束日期");
        }
        FundSettlementQO qo = new FundSettlementQO();
        qo.setCurrency(exportQO.getCurrency());
        qo.setStartDate(exportQO.getStartDate());
        qo.setEndDate(exportQO.getEndDate());
        byte[] bytes = ledgerExportService.export(queryService.platformLedgerMonths(exportQO.getWmsTenantId(), qo));
        String fileName = "资金流水-" + exportQO.getStartDate() + "-" + exportQO.getEndDate() + ".xlsx";
        String encoded;
        try { encoded = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20"); }
        catch (java.io.UnsupportedEncodingException ex) { encoded = "fund-ledger.xlsx"; }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping("/recharges/{id}/review")
    public ApiResult<Void> review(@PathVariable Long id, @Valid @RequestBody ReviewRechargeDTO dto) {
        rechargeService.reviewPlatformRecharge(id, dto);
        return ApiResult.ok();
    }

    @PostMapping("/recharges/{id}/reverse")
    public ApiResult<Void> reverse(@PathVariable Long id, @Valid @RequestBody ReverseRechargeDTO dto) {
        rechargeService.reverse(id, dto);
        return ApiResult.ok();
    }
}
