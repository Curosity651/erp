package com.erp.admin.platform.finance.controller;

import com.erp.admin.platform.finance.model.dto.ContractRefundDTO;
import com.erp.admin.platform.finance.model.dto.ServiceContractCreateDTO;
import com.erp.admin.platform.finance.model.entity.WmsContractFundLedger;
import com.erp.admin.platform.finance.model.entity.WmsServiceContract;
import com.erp.admin.platform.finance.model.vo.ContractRefundCheckVO;
import com.erp.admin.platform.finance.service.ServiceContractService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/platform-finance/service-contracts")
@PreAuthorize("@per.hasPermission('platform-finance:contract:oper')")
public class ServiceContractController {

    private final ServiceContractService service;

    @GetMapping
    public ApiResult<List<WmsServiceContract>> list() {
        return ApiResult.ok(service.list());
    }

    @GetMapping("/funds")
    public ApiResult<List<WmsContractFundLedger>> funds(@RequestParam Long contractId) {
        return ApiResult.ok(service.funds(contractId));
    }

    @PostMapping
    @Operation(summary = "创建服务合同并登记押金、认购款")
    public ApiResult<WmsServiceContract> create(@Validated @RequestBody ServiceContractCreateDTO dto) {
        return ApiResult.ok(service.create(dto));
    }

    @PostMapping("/recognize")
    public ApiResult<Void> recognize(@RequestParam Long contractId, @RequestParam String month) {
        service.recognize(contractId, month);
        return ApiResult.ok();
    }

    @PostMapping("/receive")
    @Operation(summary = "确认合同资金到账并激活货架")
    public ApiResult<WmsServiceContract> receive(@RequestParam Long contractId,
            @RequestParam(required = false) String remark) {
        return ApiResult.ok(service.receive(contractId, remark));
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消待收款合同并释放货架预留")
    public ApiResult<Void> cancel(@RequestParam Long contractId) {
        service.cancel(contractId);
        return ApiResult.ok();
    }

    @GetMapping("/refund-check")
    @Operation(summary = "核验合同退款条件")
    public ApiResult<ContractRefundCheckVO> refundCheck(@RequestParam Long contractId) {
        return ApiResult.ok(service.refundCheck(contractId));
    }

    @PostMapping("/refund")
    public ApiResult<Void> refund(@Validated @RequestBody ContractRefundDTO dto) {
        service.refund(dto);
        return ApiResult.ok();
    }
}
