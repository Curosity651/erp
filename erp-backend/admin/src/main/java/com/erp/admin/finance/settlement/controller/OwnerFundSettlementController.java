package com.erp.admin.finance.settlement.controller;

import com.erp.admin.finance.settlement.model.dto.CreateRechargeDTO;
import com.erp.admin.finance.settlement.model.entity.WmsRechargeOrder;
import com.erp.admin.finance.settlement.model.qo.FundSettlementQO;
import com.erp.admin.finance.settlement.model.vo.FundAccountVO;
import com.erp.admin.finance.settlement.model.vo.FundLedgerVO;
import com.erp.admin.finance.settlement.model.vo.RechargeOrderVO;
import com.erp.admin.finance.settlement.service.FundAccountQueryService;
import com.erp.admin.finance.settlement.service.RechargeOrderService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
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
