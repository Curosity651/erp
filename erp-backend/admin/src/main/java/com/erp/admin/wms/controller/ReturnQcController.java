package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.dto.ReturnReceiptDTO;
import com.erp.admin.wms.model.dto.ReturnProcessDTO;
import com.erp.admin.wms.model.qo.ReturnQO;
import com.erp.admin.wms.model.vo.ReturnOrderVO;
import com.erp.admin.wms.service.ReturnQcService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 海外仓平台出库作业·退货质检控制器（业务需求 1.4）。仅平台身份（service 内二次校验）。
 *
 * @author erp
 */
@Tag(name = "退货处理")
@RestController
@RequestMapping("/wms/return-qc")
@RequiredArgsConstructor
public class ReturnQcController {

    private final ReturnQcService returnQcService;

    @Operation(summary = "分页 待收货/待质检/已完成 退货单")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('wms:return-qc:oper')")
    public ApiResult<PageResult<ReturnOrderVO>> page(PageParam pageParam, ReturnQO qo) {
        return ApiResult.ok(returnQcService.page(pageParam, qo));
    }

    @Operation(summary = "退货单详情(含明细)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('wms:return-qc:oper')")
    public ApiResult<ReturnOrderVO> detail(@PathVariable("id") Long id) {
        return ApiResult.ok(returnQcService.getDetail(id));
    }

    @Operation(summary = "海外仓登记已收到的退货，按货主自动拆单")
    @PostMapping("/receipts")
    @PreAuthorize("hasAuthority('wms:return-qc:oper')")
    public ApiResult<List<Long>> registerReceipt(@Validated @RequestBody ReturnReceiptDTO dto) {
        return ApiResult.ok(returnQcService.registerReceipt(dto));
    }

    @Operation(summary = "海外仓执行货主处置决定")
    @PostMapping("/process")
    @PreAuthorize("hasAuthority('wms:return-qc:oper')")
    public ApiResult<Void> process(@Validated @RequestBody ReturnProcessDTO dto) {
        returnQcService.processDisposition(dto);
        return ApiResult.ok();
    }

    @Operation(summary = "未收到或拒收并关闭退货单")
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('wms:return-qc:oper')")
    public ApiResult<Void> close(@PathVariable("id") Long id) {
        returnQcService.close(id);
        return ApiResult.ok();
    }

    @Operation(summary = "按退货单所属仓库+分区查可用(未占用)库位，供质检上架下拉")
    @GetMapping("/available-locations")
    @PreAuthorize("hasAuthority('wms:return-qc:oper')")
    public ApiResult<java.util.List<String>> availableLocations(
            @RequestParam("returnOrderId") Long returnOrderId,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam("zone") String zone) {
        return ApiResult.ok(returnQcService.listAvailableLocations(returnOrderId, warehouseId, zone));
    }

}
