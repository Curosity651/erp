package com.erp.admin.wms.controller;

import java.util.List;

import com.erp.admin.wms.model.dto.ReturnDispositionDTO;
import com.erp.admin.wms.model.qo.ReturnInboundQO;
import com.erp.admin.wms.model.vo.ReturnInboundDetailVO;
import com.erp.admin.wms.model.vo.ReturnInboundExportVO;
import com.erp.admin.wms.model.vo.ReturnInboundPageVO;
import com.erp.admin.wms.service.ReturnInboundService;
import com.erp.admin.wms.service.ReturnQcService;
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
@Tag(name = "货主退货处理")
@RequiredArgsConstructor
public class ReturnInboundController {

    private final ReturnInboundService returnInboundService;

    private final ReturnQcService returnQcService;

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

    @PostMapping("/disposition")
    @PreAuthorize("@per.hasPermission('wms:return-inbound:read')")
    @OperationLog(bizType = "退货处理", successMessage = "处置决定已提交")
    @Operation(summary = "货主提交退货处置决定")
    public ApiResult<Void> disposition(@Validated @RequestBody ReturnDispositionDTO dto) {
        returnQcService.submitDisposition(dto);
        return ApiResult.ok();
    }

    @GetMapping("/export")
    @ResponseExcel(name = "退货入库单-#{currentDateTime()}")
    @PreAuthorize("@per.hasPermission('wms:return-inbound:read')")
    @Operation(summary = "导出退货入库单")
    public List<ReturnInboundExportVO> export(ReturnInboundQO qo) {
        return returnInboundService.listForExport(qo);
    }

}
