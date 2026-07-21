package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.dto.PickDTO;
import com.erp.admin.wms.model.qo.OutboundPickingQO;
import com.erp.admin.wms.model.vo.OutboundOrderVO;
import com.erp.admin.wms.model.vo.PickAllocationVO;
import com.erp.admin.wms.model.vo.PickListVO;
import com.erp.admin.wms.model.vo.PickerVO;
import com.erp.admin.wms.service.OutboundPickingService;
import com.erp.admin.wms.service.OutboundPickingService.PickResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
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

import java.util.List;

/**
 * 海外仓平台出库作业·下架(拣货)控制器（业务需求 1.3.1）。仅平台身份（service 内二次校验）。
 *
 * @author erp
 */
@Tag(name = "出库作业·下架(拣货)")
@RestController
@RequestMapping("/wms/outbound-picking")
@RequiredArgsConstructor
public class OutboundPickingController {

    private final OutboundPickingService outboundPickingService;

    @Operation(summary = "分页待下架/拣货中订单")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<PageResult<OutboundOrderVO>> page(PageParam pageParam, OutboundPickingQO qo) {
        return ApiResult.ok(outboundPickingService.page(pageParam, qo));
    }

    @Operation(summary = "出库单详情(含明细与可用量)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<OutboundOrderVO> detail(@PathVariable("id") Long id) {
        return ApiResult.ok(outboundPickingService.getDetail(id));
    }

    @Operation(summary = "FIFO 分配预览(只算不锁)")
    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<List<PickAllocationVO>> preview(@PathVariable("id") Long id) {
        return ApiResult.ok(outboundPickingService.preview(id));
    }

    @Operation(summary = "确认下架(FIFO锁定/生成拣货单/转PICKING;缺货整单挂起)")
    @PostMapping("/pick")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<Void> pick(@Validated @RequestBody PickDTO dto) {
        PickResult r = outboundPickingService.pick(dto);
        if (!r.isOk()) {
            // 状态已落 BACKORDER（事务已提交），此处仅向前端返回失败提示
            throw new BusinessException(40030, r.getMessage());
        }
        return ApiResult.ok();
    }

    @Operation(summary = "拣货单")
    @GetMapping("/{id}/pick-list")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<PickListVO> pickList(@PathVariable("id") Long id) {
        return ApiResult.ok(outboundPickingService.getPickList(id));
    }

    @Operation(summary = "拣货员选项")
    @GetMapping("/pickers")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<List<PickerVO>> pickers() {
        return ApiResult.ok(outboundPickingService.listPickers());
    }

}
