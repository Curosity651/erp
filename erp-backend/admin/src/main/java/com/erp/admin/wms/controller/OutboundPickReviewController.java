package com.erp.admin.wms.controller;

import java.time.LocalDate;

import com.erp.admin.wms.model.dto.OutboundPickReviewConfirmDTO;
import com.erp.admin.wms.model.entity.WmsOutboundPickReview;
import com.erp.admin.wms.model.vo.OutboundPickReviewSummaryVO;
import com.erp.admin.wms.service.OutboundPickReviewService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/outbound-pick-review")
@RequiredArgsConstructor
public class OutboundPickReviewController {

	private final OutboundPickReviewService service;
	private final PrincipalAttributeAccessor principalAttributeAccessor;

	@GetMapping("/summary")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<OutboundPickReviewSummaryVO> summary(
			@RequestParam("workDate")
			@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate workDate,
			@RequestParam(value = "warehouseId", required = false) Long warehouseId) {
		return ApiResult.ok(service.summary(workDate, warehouseId));
	}

	@PostMapping("/confirm")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<WmsOutboundPickReview> confirm(
			@Validated @RequestBody OutboundPickReviewConfirmDTO dto) {
		return ApiResult.ok(service.confirm(dto, principalAttributeAccessor.getUserId()));
	}
}
