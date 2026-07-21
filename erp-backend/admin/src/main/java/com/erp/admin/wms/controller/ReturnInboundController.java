package com.erp.admin.wms.controller;

import java.util.List;

import com.erp.admin.wms.model.dto.ReturnInboundDTO;
import com.erp.admin.wms.model.qo.ReturnInboundQO;
import com.erp.admin.wms.model.qo.ReturnableOrderQO;
import com.erp.admin.wms.model.vo.ReturnInboundDetailVO;
import com.erp.admin.wms.model.vo.ReturnInboundExportVO;
import com.erp.admin.wms.model.vo.ReturnInboundPageVO;
import com.erp.admin.wms.model.vo.ReturnableOrderVO;
import com.erp.admin.wms.service.ReturnInboundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.fastexcel.annotation.ResponseExcel;
import org.ballcat.log.operation.annotation.OperationLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退货入库单管理
 * 重构后：简化状态流转，改为一步到位的退货入库流程
 *
 * @author erp
 */
@RestController
@RequestMapping("/wms/return-inbound")
@Tag(name = "退货入库单管理")
@RequiredArgsConstructor
public class ReturnInboundController {

    private final ReturnInboundService returnInboundService;

    @GetMapping("/page")
    @PreAuthorize("@per.hasPermission('wms:return-inbound:read')")
    @Operation(summary = "分页查询退货入库单")
    public ApiResult<PageResult<ReturnInboundPageVO>> getPage(PageParam pageParam, ReturnInboundQO qo) {
        return ApiResult.ok(returnInboundService.queryPage(pageParam, qo));
    }

    @GetMapping("/detail")
    @PreAuthorize("@per.hasPermission('wms:return-inbound:read')")
    @Operation(summary = "获取退货入库单详情")
    public ApiResult<ReturnInboundDetailVO> getDetail(@RequestParam Long id) {
        return ApiResult.ok(returnInboundService.getDetail(id));
    }

    @GetMapping("/returnable-orders")
    @PreAuthorize("@per.hasPermission('wms:return-inbound:read')")
    @Operation(summary = "获取可退货订单列表")
    public ApiResult<PageResult<ReturnableOrderVO>> getReturnableOrders(PageParam pageParam, ReturnableOrderQO qo) {
        return ApiResult.ok(returnInboundService.getReturnableOrders(pageParam, qo));
    }

    @PostMapping
    @PreAuthorize("@per.hasPermission('wms:return-inbound:add')")
    @OperationLog(bizType = "退货入库单管理", successMessage = "退货入库成功")
    @Operation(summary = "创建退货入库单（一步到位）")
    public ApiResult<Long> create(@Validated @RequestBody ReturnInboundDTO dto) {
        return ApiResult.ok(returnInboundService.create(dto));
    }

    @GetMapping("/export")
    @ResponseExcel(name = "退货入库单-#{currentDateTime()}")
    @PreAuthorize("@per.hasPermission('wms:return-inbound:read')")
    @Operation(summary = "导出退货入库单")
    public List<ReturnInboundExportVO> export(ReturnInboundQO qo) {
        return returnInboundService.listForExport(qo);
    }

}
