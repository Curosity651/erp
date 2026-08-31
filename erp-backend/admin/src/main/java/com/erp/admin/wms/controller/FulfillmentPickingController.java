package com.erp.admin.wms.controller;

import java.time.LocalDateTime;
import java.util.List;

import com.erp.admin.wms.model.dto.FulfillmentPickScanDTO;
import com.erp.admin.wms.model.dto.FulfillmentPickTaskCreateDTO;
import com.erp.admin.wms.model.dto.FulfillmentPickTaskQueryDTO;
import com.erp.admin.wms.model.dto.FulfillmentPickExceptionDTO;
import com.erp.admin.wms.model.dto.FulfillmentSimplifiedCompleteDTO;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import com.erp.admin.wms.model.vo.FulfillmentDispatchResultVO;
import com.erp.admin.wms.model.vo.FulfillmentPickTaskDetailVO;
import com.erp.admin.wms.service.FulfillmentDispatchService;
import com.erp.admin.wms.service.FulfillmentPickingService;
import com.erp.admin.wms.service.FulfillmentShippingService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/fulfillment-picking")
@RequiredArgsConstructor
public class FulfillmentPickingController {
	private final FulfillmentPickingService service;
	private final FulfillmentShippingService shippingService;
	private final FulfillmentDispatchService dispatchService;
	private final PrincipalAttributeAccessor principalAttributeAccessor;

	@GetMapping("/shelf-orders")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<List<WmsFulfillmentOrder>> shelfOrders(
			@RequestParam(value = "startTime", required = false)
			@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
			@RequestParam(value = "endTime", required = false)
			@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
			@RequestParam(value = "erpTenantId", required = false) Long erpTenantId,
			@RequestParam(value = "logisticsProductId", required = false) Long logisticsProductId,
			@RequestParam(value = "warehouseId", required = false) Long warehouseId) {
		return ApiResult.ok(service.listShelfOrders(startTime, endTime, erpTenantId,
				logisticsProductId, warehouseId));
	}

	@PostMapping("/accept")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<FulfillmentDispatchResultVO> accept(@RequestBody List<Long> orderIds) {
		return ApiResult.ok(dispatchService.dispatch(orderIds,
				principalAttributeAccessor.getUserId()));
	}

	@PostMapping("/redispatch")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<FulfillmentDispatchResultVO> redispatch(@RequestBody List<Long> orderIds) {
		return ApiResult.ok(dispatchService.redispatch(orderIds,
				principalAttributeAccessor.getUserId()));
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
	public ApiResult<List<WmsFulfillmentPickTask>> tasks(FulfillmentPickTaskQueryDTO query) {
		return ApiResult.ok(service.listTasks(query));
	}

	@PostMapping("/tasks/{id}/claim")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> claim(@PathVariable("id") Long id) {
		service.claimTask(id, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@PostMapping("/tasks/{id}/release")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> release(@PathVariable("id") Long id) {
		service.releaseTask(id, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@PostMapping("/tasks/{id}/transfer")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> transfer(@PathVariable("id") Long id,
			@RequestBody java.util.Map<String, Long> body) {
		service.transferTask(id, body.get("operatorId"));
		return ApiResult.ok();
	}

	@GetMapping("/tasks/{id}")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<FulfillmentPickTaskDetailVO> detail(@PathVariable("id") Long id,
			@RequestParam(value = "fulfillmentOrderId", required = false)
			Long fulfillmentOrderId) {
		return ApiResult.ok(service.detail(id, fulfillmentOrderId));
	}

	@PostMapping("/tasks/scan")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> scan(@Validated @RequestBody FulfillmentPickScanDTO dto) {
		service.scan(dto, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@PostMapping("/tasks/{id}/simplified-start")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> startSimplified(@PathVariable("id") Long id) {
		service.startSimplifiedPicking(id, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@PostMapping("/tasks/{id}/simplified-complete")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> completeSimplified(@PathVariable("id") Long id,
			@Validated @RequestBody FulfillmentSimplifiedCompleteDTO dto) {
		shippingService.completeSimplifiedTask(id, dto.getEvidenceFileIds(),
				principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@PostMapping("/tasks/{taskId}/orders/{orderId}/exception")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> markException(@PathVariable("taskId") Long taskId,
			@PathVariable("orderId") Long orderId,
			@Validated @RequestBody FulfillmentPickExceptionDTO dto) {
		service.markException(taskId, orderId, dto, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@PostMapping("/tasks/{taskId}/orders/{orderId}/restore")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> restoreException(@PathVariable("taskId") Long taskId,
			@PathVariable("orderId") Long orderId) {
		service.restoreException(taskId, orderId, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@PostMapping("/tasks/{taskId}/orders/{orderId}/cancel")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> cancelException(@PathVariable("taskId") Long taskId,
			@PathVariable("orderId") Long orderId) {
		service.cancelException(taskId, orderId, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}
}
