package com.erp.admin.finance.settlement.controller;

import com.erp.admin.finance.settlement.model.dto.CreateRechargeDTO;
import com.erp.admin.finance.settlement.model.entity.WmsRechargeOrder;
import com.erp.admin.finance.settlement.model.qo.FundSettlementQO;
import com.erp.admin.finance.settlement.model.qo.FundLedgerExportQO;
import com.erp.admin.finance.settlement.model.vo.FundAccountVO;
import com.erp.admin.finance.settlement.model.vo.FundLedgerMonthVO;
import com.erp.admin.finance.settlement.model.vo.FundLedgerVO;
import com.erp.admin.finance.settlement.model.vo.RechargeOrderVO;
import com.erp.admin.finance.settlement.service.FundAccountQueryService;
import com.erp.admin.finance.settlement.service.FundLedgerExportService;
import com.erp.admin.finance.settlement.service.RechargeOrderService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/fund-settlement/owner")
@RequiredArgsConstructor
public class OwnerFundSettlementController {
    private final FundAccountQueryService queryService;
    private final RechargeOrderService rechargeService;
    private final FundLedgerExportService ledgerExportService;

    @GetMapping("/accounts")
    public ApiResult<List<FundAccountVO>> accounts() { return ApiResult.ok(queryService.ownerAccounts()); }

    @GetMapping("/recharges")
    public ApiResult<List<RechargeOrderVO>> recharges(FundSettlementQO qo) {
        return ApiResult.ok(queryService.ownerRecharges(qo));
    }

    @GetMapping("/ledger")
    public ApiResult<List<FundLedgerVO>> ledger(FundSettlementQO qo) {
        return ApiResult.ok(queryService.ownerLedger(qo));
    }

    @GetMapping("/ledger/months")
    public ApiResult<List<FundLedgerMonthVO>> ledgerMonths(FundSettlementQO qo) {
        return ApiResult.ok(queryService.ownerLedgerMonths(qo));
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
        byte[] bytes = ledgerExportService.export(queryService.ownerLedgerMonths(qo));
        String fileName = "WMS资金流水-" + exportQO.getStartDate() + "-" + exportQO.getEndDate() + ".xlsx";
        String encoded;
        try { encoded = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20"); }
        catch (java.io.UnsupportedEncodingException ex) { encoded = "wms-fund-ledger.xlsx"; }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping("/recharges")
    public ApiResult<WmsRechargeOrder> create(@Valid @RequestBody CreateRechargeDTO dto) {
        return ApiResult.ok(rechargeService.createOwnerRecharge(dto));
    }

    @PostMapping("/recharges/{id}/cancel")
    public ApiResult<Void> cancel(@PathVariable Long id) {
        rechargeService.cancel(id);
        return ApiResult.ok();
    }
}
