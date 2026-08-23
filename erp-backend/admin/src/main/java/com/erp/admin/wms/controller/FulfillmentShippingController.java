package com.erp.admin.wms.controller;

import java.util.List;
import java.util.Map;

import com.erp.admin.wms.model.dto.FulfillmentPackDTO;
import com.erp.admin.wms.model.dto.FulfillmentLogisticsFeeDTO;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.vo.FulfillmentBatchResultVO;
import com.erp.admin.wms.service.FulfillmentShippingService;
import com.erp.admin.wms.service.platform.PlatformLabelResult;
import lombok.RequiredArgsConstructor;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/fulfillment-shipping")
@RequiredArgsConstructor
public class FulfillmentShippingController {
	private final FulfillmentShippingService service;
	private final PrincipalAttributeAccessor principalAttributeAccessor;

	@GetMapping
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<List<WmsFulfillmentOrder>> list() {
		return ApiResult.ok(service.listWorkOrders());
	}

	@PostMapping("/{id}/label")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<PlatformLabelResult> label(@PathVariable("id") Long id) {
		return ApiResult.ok(service.printLabel(id, principalAttributeAccessor.getUserId()));
	}

	@PostMapping("/{id}/label/verify")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> verify(@PathVariable("id") Long id,
			@RequestBody Map<String, String> body) {
		service.verifyLabel(id, body.get("barcode"), principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@PostMapping("/{id}/pack")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> pack(@PathVariable("id") Long id,
			@Validated @RequestBody FulfillmentPackDTO dto) {
		service.pack(id, dto, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@PostMapping("/{id}/logistics-fee")
	@PreAuthorize("@per.hasPermission('wms:logistics-product:edit')")
	public ApiResult<Void> adjustLogisticsFee(@PathVariable("id") Long id,
			@Validated @RequestBody FulfillmentLogisticsFeeDTO dto) {
		service.adjustLogisticsFee(id, dto);
		return ApiResult.ok();
	}

	@PostMapping("/ship")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<FulfillmentBatchResultVO> ship(@RequestBody List<Long> ids) {
		return ApiResult.ok(service.ship(ids));
	}
}
