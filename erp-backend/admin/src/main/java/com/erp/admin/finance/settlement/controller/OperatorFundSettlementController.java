package com.erp.admin.finance.settlement.controller;

import com.erp.admin.finance.settlement.model.dto.CreateRechargeDTO;
import com.erp.admin.finance.settlement.model.dto.ReviewRechargeDTO;
import com.erp.admin.finance.settlement.model.dto.ReverseRechargeDTO;
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
@RequestMapping("/fund-settlement/operator")
@RequiredArgsConstructor
public class OperatorFundSettlementController {
    private final FundAccountQueryService queryService;
    private final RechargeOrderService rechargeService;

    @GetMapping("/owner/accounts")
    public ApiResult<List<FundAccountVO>> ownerAccounts(FundSettlementQO qo) {
        return ApiResult.ok(queryService.operatorOwnerAccounts(qo));
    }

    @GetMapping("/owner/recharges")
    public ApiResult<List<RechargeOrderVO>> ownerRecharges(FundSettlementQO qo) {
        return ApiResult.ok(queryService.operatorOwnerRecharges(qo));
    }

    @GetMapping("/owner/ledger")
    public ApiResult<List<FundLedgerVO>> ownerLedger(@RequestParam Long erpTenantId, FundSettlementQO qo) {
        return ApiResult.ok(queryService.operatorOwnerLedger(erpTenantId, qo));
    }

    @PostMapping("/owner/recharges/{id}/review")
    public ApiResult<Void> reviewOwner(@PathVariable Long id, @Valid @RequestBody ReviewRechargeDTO dto) {
        rechargeService.reviewOwnerRecharge(id, dto);
        return ApiResult.ok();
    }

    @PostMapping("/owner/recharges/{id}/reverse")
    public ApiResult<Void> reverseOwner(@PathVariable Long id, @Valid @RequestBody ReverseRechargeDTO dto) {
        rechargeService.reverse(id, dto);
        return ApiResult.ok();
    }

    @GetMapping("/platform/accounts")
    public ApiResult<List<FundAccountVO>> platformAccounts() {
        return ApiResult.ok(queryService.operatorPlatformAccounts());
    }

    @GetMapping("/platform/recharges")
    public ApiResult<List<RechargeOrderVO>> platformRecharges(FundSettlementQO qo) {
        return ApiResult.ok(queryService.operatorPlatformRecharges(qo));
    }

    @GetMapping("/platform/ledger")
    public ApiResult<List<FundLedgerVO>> platformLedger(FundSettlementQO qo) {
        return ApiResult.ok(queryService.operatorPlatformLedger(qo));
    }

    @PostMapping("/platform/recharges")
    public ApiResult<WmsRechargeOrder> createPlatform(@Valid @RequestBody CreateRechargeDTO dto) {
        return ApiResult.ok(rechargeService.createPlatformRecharge(dto));
    }

    @PostMapping("/platform/recharges/{id}/cancel")
    public ApiResult<Void> cancelPlatform(@PathVariable Long id) {
        rechargeService.cancel(id);
        return ApiResult.ok();
    }
}
