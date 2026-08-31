package com.erp.admin.wms.controller;

import com.erp.admin.wms.config.WmsCoreModeGuard;
import com.erp.admin.wms.model.dto.PickDTO;
import com.erp.admin.wms.model.qo.OutboundPickingQO;
import com.erp.admin.wms.model.dto.BatchPickDTO;
import com.erp.admin.wms.model.dto.BatchPickPreviewDTO;
import com.erp.admin.wms.model.dto.PackageScanDTO;
import com.erp.admin.wms.model.dto.PickExceptionDTO;
import com.erp.admin.wms.model.dto.PickLineScanDTO;
import com.erp.admin.wms.model.dto.PickReturnScanDTO;
import com.erp.admin.wms.model.dto.ResolvePickExceptionDTO;
import com.erp.admin.wms.model.vo.OutboundOrderVO;
import com.erp.admin.wms.model.vo.PickAllocationVO;
import com.erp.admin.wms.model.vo.PickListVO;
import com.erp.admin.wms.model.vo.PickerVO;
import com.erp.admin.wms.model.vo.BatchPickPreviewVO;
import com.erp.admin.wms.model.vo.BatchPickResultVO;
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
import org.ballcat.security.core.PrincipalAttributeAccessor;
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
    private final PrincipalAttributeAccessor principalAttributeAccessor;
    private final WmsCoreModeGuard coreModeGuard;

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
        coreModeGuard.assertLegacyWriteAllowed("旧出库拣货");
        PickResult r = outboundPickingService.pick(dto);
        if (!r.isOk()) {
            // 状态已落 BACKORDER（事务已提交），此处仅向前端返回失败提示
            throw new BusinessException(40030, r.getMessage());
        }
        return ApiResult.ok();
    }

    @Operation(summary = "批量拣货任务自动拆分预览")
    @PostMapping("/batch-preview")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<BatchPickPreviewVO> batchPreview(@Validated @RequestBody BatchPickPreviewDTO dto) {
        coreModeGuard.assertLegacyWriteAllowed("旧批量拣货预览");
        return ApiResult.ok(outboundPickingService.previewBatch(dto));
    }

    @Operation(summary = "批量创建拣货任务")
    @PostMapping("/batch-create")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<BatchPickResultVO> batchCreate(@Validated @RequestBody BatchPickDTO dto) {
        coreModeGuard.assertLegacyWriteAllowed("旧批量拣货任务创建");
        return ApiResult.ok(outboundPickingService.createBatch(dto));
    }

    @Operation(summary = "确认拣货任务完成并进入待打包")
    @PostMapping("/tasks/{taskId}/complete")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<Void> completeTask(@PathVariable("taskId") Long taskId) {
        coreModeGuard.assertLegacyWriteAllowed("旧拣货任务完成");
        outboundPickingService.completeTask(taskId);
        return ApiResult.ok();
    }

    @Operation(summary = "扫描或手工登记一条实拣数量")
    @PostMapping("/tasks/scan")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper') and (#dto.manual != true or hasAuthority('wms:outbound-exec:supervise'))")
    public ApiResult<Void> scanLine(@Validated @RequestBody PickLineScanDTO dto) {
        coreModeGuard.assertLegacyWriteAllowed("旧拣货扫描");
        outboundPickingService.scanPickLine(dto, principalAttributeAccessor.getUserId());
        return ApiResult.ok();
    }

    @Operation(summary = "扫描已拣商品返库")
    @PostMapping("/tasks/return/scan")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper') and (#dto.manual != true or hasAuthority('wms:outbound-exec:supervise'))")
    public ApiResult<Void> scanReturn(@Validated @RequestBody PickReturnScanDTO dto) {
        coreModeGuard.assertLegacyWriteAllowed("旧拣货返库");
        outboundPickingService.scanReturnLine(dto, principalAttributeAccessor.getUserId());
        return ApiResult.ok();
    }

    @Operation(summary = "报告拣货缺货异常")
    @PostMapping("/tasks/exception")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<Void> reportException(@Validated @RequestBody PickExceptionDTO dto) {
        coreModeGuard.assertLegacyWriteAllowed("旧拣货异常登记");
        outboundPickingService.reportPickException(dto, principalAttributeAccessor.getUserId());
        return ApiResult.ok();
    }

    @Operation(summary = "查询异常明细的替代批次")
    @GetMapping("/tasks/{taskId}/lines/{lineId}/alternatives")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<List<PickAllocationVO>> alternatives(@PathVariable("taskId") Long taskId,
            @PathVariable("lineId") Long lineId) {
        return ApiResult.ok(outboundPickingService.listPickAlternatives(taskId, lineId));
    }

    @Operation(summary = "主管处理拣货异常")
    @PostMapping("/tasks/exception/resolve")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<Void> resolveException(@Validated @RequestBody ResolvePickExceptionDTO dto) {
        coreModeGuard.assertLegacyWriteAllowed("旧拣货异常处理");
        outboundPickingService.resolvePickException(dto, principalAttributeAccessor.getUserId());
        return ApiResult.ok();
    }

    @Operation(summary = "扫描核对一个平台订单格口商品")
    @PostMapping("/tasks/{taskId}/packages/scan")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper') and (#dto.manual != true or hasAuthority('wms:outbound-exec:supervise'))")
    public ApiResult<Void> scanPackageSort(@PathVariable("taskId") Long taskId,
            @Validated @RequestBody PackageScanDTO dto) {
        coreModeGuard.assertLegacyWriteAllowed("旧包裹分货扫描");
        outboundPickingService.scanPackageSort(taskId, dto, principalAttributeAccessor.getUserId());
        return ApiResult.ok();
    }

	@Operation(summary = "确认一个平台订单格口完成分货")
	@PostMapping("/tasks/{taskId}/packages/{packageId}/sort")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> confirmPackageSort(@PathVariable("taskId") Long taskId,
			@PathVariable("packageId") Long packageId) {
		coreModeGuard.assertLegacyWriteAllowed("旧包裹分货完成");
		outboundPickingService.confirmPackageSort(taskId, packageId, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@Operation(summary = "未开始分货时跳过格口，直接进入逐单复核打包")
	@PostMapping("/tasks/{taskId}/skip-sorting")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> skipSorting(@PathVariable("taskId") Long taskId) {
		coreModeGuard.assertLegacyWriteAllowed("旧包裹跳过分货");
		outboundPickingService.skipSorting(taskId);
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
