package com.erp.admin.order.controller;

import com.erp.admin.order.model.dto.SubmitFulfillmentDTO;
import com.erp.admin.order.service.ErpOrderFulfillmentSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order/fulfillment")
@Tag(name = "订单仓库履约")
@RequiredArgsConstructor
public class FulfillmentSubmissionController {
	private final ErpOrderFulfillmentSubmissionService submissionService;

	@PostMapping("/submit")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	@Operation(summary = "确认并提交海外仓履约")
	public ApiResult<Long> submit(@Validated @RequestBody SubmitFulfillmentDTO dto) {
		return ApiResult.ok(submissionService.submit(dto));
	}

	@PostMapping("/{orderId}/cancel")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	@Operation(summary = "下架前取消仓库履约")
	public ApiResult<Void> cancel(@PathVariable Long orderId, @RequestBody(required = false) CancelRequest request) {
		submissionService.cancel(orderId, request == null ? null : request.getReason());
		return ApiResult.ok();
	}

	@Data
	public static class CancelRequest {
		private String reason;
	}
}
