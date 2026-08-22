package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.dto.AdjustmentDTO;
import com.erp.admin.wms.model.qo.AdjustmentQO;
import com.erp.admin.wms.model.vo.AdjustmentDetailVO;
import com.erp.admin.wms.model.vo.AdjustmentPageVO;
import com.erp.admin.wms.model.vo.PalletSummaryVO;
import com.erp.admin.wms.model.vo.ScrapBatchVO;
import com.erp.admin.wms.service.AdjustmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.log.operation.annotation.OperationLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报废单管理（原库存调整单收敛为报废，货主审批流）。
 *
 * <p>平台发起/撤销、货主确认/驳回；分页与详情按身份收窄（货主只见自己）。
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/adjustment")
@Tag(name = "报废单管理")
public class AdjustmentController {

	private final AdjustmentService adjustmentService;

	@Operation(summary = "分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('wms:adjustment:read')")
	public ApiResult<PageResult<AdjustmentPageVO>> getPage(PageParam pageParam, AdjustmentQO qo) {
		return ApiResult.ok(adjustmentService.queryPage(pageParam, qo));
	}

	@Operation(summary = "获取详情")
	@GetMapping("/detail")
	@PreAuthorize("@per.hasPermission('wms:adjustment:read')")
	public ApiResult<AdjustmentDetailVO> getDetail(@RequestParam Long id) {
		return ApiResult.ok(adjustmentService.getDetail(id));
	}

	@Operation(summary = "查询所选货主在本仓不良品区的可报废批次")
	@GetMapping("/scrap-batches")
	@PreAuthorize("@per.hasPermission('wms:adjustment:add')")
	public ApiResult<List<ScrapBatchVO>> scrapBatches(@RequestParam Long erpTenantId,
			@RequestParam Long warehouseId) {
		return ApiResult.ok(adjustmentService.listScrapCandidates(erpTenantId, warehouseId));
	}

	@Operation(summary = "平台发起报废")
	@OperationLog(bizType = "报废单管理", successMessage = "发起成功")
	@PostMapping
	@PreAuthorize("@per.hasPermission('wms:adjustment:add')")
	public ApiResult<Long> create(@Validated @RequestBody AdjustmentDTO dto) {
		return ApiResult.ok(adjustmentService.create(dto));
	}

	@Operation(summary = "平台撤销报废")
	@OperationLog(bizType = "报废单管理", successMessage = "撤销成功")
	@PatchMapping("/cancel")
	@PreAuthorize("@per.hasPermission('wms:adjustment:cancel')")
	public ApiResult<Void> cancel(@RequestParam Long id) {
		adjustmentService.cancel(id);
		return ApiResult.ok();
	}

	@Operation(summary = "货主同意报废")
	@OperationLog(bizType = "报废单管理", successMessage = "已同意报废")
	@PatchMapping("/owner-confirm")
	@PreAuthorize("@per.hasPermission('wms:scrap:confirm')")
	public ApiResult<Void> ownerConfirm(@RequestParam Long id) {
		adjustmentService.ownerConfirm(id);
		return ApiResult.ok();
	}

	@Operation(summary = "海外仓确认实际销毁")
	@OperationLog(bizType = "报废单管理", successMessage = "已确认实际销毁")
	@PatchMapping("/destroy")
	@PreAuthorize("@per.hasPermission('wms:adjustment:confirm')")
	public ApiResult<Void> destroy(@RequestParam Long id) {
		adjustmentService.destroy(id);
		return ApiResult.ok();
	}

	@Operation(summary = "查询报废后需要重新打印的托盘标签")
	@GetMapping("/pallets")
	@PreAuthorize("@per.hasPermission('wms:adjustment:read')")
	public ApiResult<List<PalletSummaryVO>> pallets(@RequestParam Long id) {
		return ApiResult.ok(adjustmentService.listPrintablePallets(id));
	}

	@Operation(summary = "货主驳回报废")
	@OperationLog(bizType = "报废单管理", successMessage = "已驳回")
	@PatchMapping("/owner-reject")
	@PreAuthorize("@per.hasPermission('wms:scrap:confirm')")
	public ApiResult<Void> ownerReject(@RequestParam Long id,
			@RequestParam(required = false) String reason) {
		adjustmentService.ownerReject(id, reason);
		return ApiResult.ok();
	}

	@Operation(summary = "删除报废单")
	@OperationLog(bizType = "报废单管理", successMessage = "删除成功")
	@DeleteMapping
	@PreAuthorize("@per.hasPermission('wms:adjustment:del')")
	public ApiResult<Void> delete(@RequestBody List<Long> ids) {
		adjustmentService.delete(ids);
		return ApiResult.ok();
	}

}
