package com.erp.admin.platform.finance.controller;

import com.erp.admin.platform.finance.model.dto.DisputeBillDTO;
import com.erp.admin.platform.finance.model.dto.BillAdjustmentDTO;
import com.erp.admin.platform.finance.model.dto.GenerateBillDTO;
import com.erp.admin.platform.finance.model.dto.ManualBillingDTO;
import com.erp.admin.platform.finance.model.dto.PayBillDTO;
import com.erp.admin.platform.finance.model.entity.WmsBillingRecord;
import com.erp.admin.platform.finance.model.entity.WmsFeeRateCard;
import com.erp.admin.platform.finance.model.qo.MonthlyBillQO;
import com.erp.admin.platform.finance.model.vo.GenerateBillResultVO;
import com.erp.admin.platform.finance.model.vo.MonthlyBillVO;
import com.erp.admin.platform.finance.service.MonthlyBillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

/**
 * 平台财务·应收账单控制器（链路一，业务需求 1.7）。仅平台身份（service 内二次校验）。
 *
 * @author erp
 */
@Tag(name = "平台财务·应收账单")
@RestController
@RequestMapping("/platform-finance/monthly-bill")
@RequiredArgsConstructor
public class MonthlyBillController {

    private final MonthlyBillService monthlyBillService;

    @Operation(summary = "分页查询账单")
    @PostMapping("/page")
    @PreAuthorize("hasAuthority('platform-finance:settle')")
    public ApiResult<PageResult<MonthlyBillVO>> page(PageParam pageParam, @RequestBody MonthlyBillQO qo) {
        return ApiResult.ok(monthlyBillService.page(pageParam, qo));
    }

    @Operation(summary = "账单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('platform-finance:settle')")
    public ApiResult<MonthlyBillVO> detail(@PathVariable("id") Long id) {
        return ApiResult.ok(monthlyBillService.getDetail(id));
    }

    @Operation(summary = "查询服务商当前有效收费标准")
    @GetMapping("/rates")
    @PreAuthorize("hasAuthority('platform-finance:settle')")
    public ApiResult<List<WmsFeeRateCard>> rates(@RequestParam Long wmsTenantId) {
        return ApiResult.ok(monthlyBillService.listRates(wmsTenantId));
    }

    @Operation(summary = "登记配送、退货和验货补充费用")
    @PostMapping("/manual-charge")
    @PreAuthorize("hasAuthority('platform-finance:settle')")
    public ApiResult<WmsBillingRecord> manualCharge(@Validated @RequestBody ManualBillingDTO dto) {
        return ApiResult.ok(monthlyBillService.addManualCharge(dto));
    }

    @Operation(summary = "为待复核账单登记补收或冲减")
    @PostMapping("/{id}/adjustment")
    @PreAuthorize("hasAuthority('platform-finance:settle')")
    public ApiResult<MonthlyBillVO> adjustment(@PathVariable("id") Long id,
            @Validated @RequestBody BillAdjustmentDTO dto) {
        return ApiResult.ok(monthlyBillService.addAdjustment(id, dto));
    }

    @Operation(summary = "生成/重算账单(草稿覆盖，已确认/已付款跳过)")
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('platform-finance:settle')")
    public ApiResult<GenerateBillResultVO> generate(@Validated @RequestBody GenerateBillDTO dto) {
        return ApiResult.ok(monthlyBillService.generate(dto));
    }

    @Operation(summary = "草稿/争议→已确认")
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('platform-finance:settle')")
    public ApiResult<MonthlyBillVO> confirm(@PathVariable("id") Long id) {
        return ApiResult.ok(monthlyBillService.confirm(id));
    }

    @Operation(summary = "已确认→已付款")
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('platform-finance:settle')")
    public ApiResult<MonthlyBillVO> pay(@PathVariable("id") Long id,
            @Validated @RequestBody PayBillDTO dto) {
        return ApiResult.ok(monthlyBillService.pay(id, dto.getPaymentVoucherFileId()));
    }

    @Operation(summary = "已确认→争议")
    @PostMapping("/{id}/dispute")
    @PreAuthorize("hasAuthority('platform-finance:settle')")
    public ApiResult<MonthlyBillVO> dispute(@PathVariable("id") Long id, @RequestBody DisputeBillDTO dto) {
        return ApiResult.ok(monthlyBillService.dispute(id, dto == null ? null : dto.getRemark()));
    }

}
