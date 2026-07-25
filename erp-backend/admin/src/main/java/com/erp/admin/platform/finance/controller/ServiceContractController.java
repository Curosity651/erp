package com.erp.admin.platform.finance.controller;

import com.erp.admin.platform.finance.model.dto.ContractRefundDTO;
import com.erp.admin.platform.finance.model.dto.ServiceContractCreateDTO;
import com.erp.admin.platform.finance.model.entity.WmsContractFundLedger;
import com.erp.admin.platform.finance.model.entity.WmsServiceContract;
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

    @PostMapping("/refund")
    public ApiResult<Void> refund(@Validated @RequestBody ContractRefundDTO dto) {
        service.refund(dto);
        return ApiResult.ok();
    }
}

