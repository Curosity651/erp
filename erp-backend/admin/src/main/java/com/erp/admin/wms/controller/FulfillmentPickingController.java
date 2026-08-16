package com.erp.admin.wms.controller;

import java.util.List;

import com.erp.admin.wms.model.dto.FulfillmentPickScanDTO;
import com.erp.admin.wms.model.dto.FulfillmentPickTaskCreateDTO;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import com.erp.admin.wms.model.vo.FulfillmentBatchResultVO;
import com.erp.admin.wms.model.vo.FulfillmentPickTaskDetailVO;
import com.erp.admin.wms.service.FulfillmentPickingService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/fulfillment-picking")
@RequiredArgsConstructor
public class FulfillmentPickingController {
	private final FulfillmentPickingService service;
	private final PrincipalAttributeAccessor principalAttributeAccessor;

	@GetMapping("/shelf-orders")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<List<WmsFulfillmentOrder>> shelfOrders() {
		return ApiResult.ok(service.listShelfOrders());
	}

	@PostMapping("/accept")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<FulfillmentBatchResultVO> accept(@RequestBody List<Long> orderIds) {
		return ApiResult.ok(service.accept(orderIds));
	}

	@PostMapping("/tasks")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<WmsFulfillmentPickTask> createTask(
			@Validated @RequestBody FulfillmentPickTaskCreateDTO dto) {
		return ApiResult.ok(service.createTask(dto.getFulfillmentOrderIds(),
				principalAttributeAccessor.getUserId()));
	}

	@GetMapping("/tasks")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<List<WmsFulfillmentPickTask>> tasks() {
		return ApiResult.ok(service.listTasks());
	}

	@GetMapping("/tasks/{id}")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<FulfillmentPickTaskDetailVO> detail(@PathVariable("id") Long id) {
		return ApiResult.ok(service.detail(id));
	}

	@PostMapping("/tasks/scan")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> scan(@Validated @RequestBody FulfillmentPickScanDTO dto) {
		service.scan(dto);
		return ApiResult.ok();
	}
}
