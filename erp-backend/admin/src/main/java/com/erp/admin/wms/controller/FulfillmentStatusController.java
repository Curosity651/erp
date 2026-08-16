package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.dto.FulfillmentCancelReturnScanDTO;
import com.erp.admin.wms.service.FulfillmentStatusSyncService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/fulfillment-status")
@RequiredArgsConstructor
public class FulfillmentStatusController {
	private final FulfillmentStatusSyncService service;

	@PostMapping("/cancel-return/scan")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> scanReturn(@Validated @RequestBody FulfillmentCancelReturnScanDTO dto) {
		service.scanReturn(dto);
		return ApiResult.ok();
	}
}
