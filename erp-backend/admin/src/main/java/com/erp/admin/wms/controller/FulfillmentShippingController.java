package com.erp.admin.wms.controller;

import java.util.List;
import java.util.Map;

import com.erp.admin.wms.model.dto.FulfillmentPackDTO;
import com.erp.admin.wms.model.dto.FulfillmentLogisticsFeeDTO;
import com.erp.admin.wms.model.dto.FulfillmentHandoverDTO;
import com.erp.admin.wms.model.dto.OutboundHandoverCreateDTO;
import com.erp.admin.wms.model.dto.TransportExpenseDTO;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsTransportExpenseRecord;
import com.erp.admin.wms.model.qo.FulfillmentShippingQuery;
import com.erp.admin.wms.model.vo.FulfillmentBatchResultVO;
import com.erp.admin.wms.model.vo.FulfillmentShippingOrderVO;
import com.erp.admin.wms.model.vo.OutboundHandoverOrderVO;
import com.erp.admin.wms.service.FulfillmentShippingService;
import com.erp.admin.wms.service.FulfillmentHandoverService;
import com.erp.admin.wms.service.TransportExpenseService;
import com.erp.admin.wms.service.platform.PlatformLabelResult;
import lombok.RequiredArgsConstructor;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/wms/fulfillment-shipping")
@RequiredArgsConstructor
public class FulfillmentShippingController {
	private final FulfillmentShippingService service;
	private final FulfillmentHandoverService handoverService;
	private final TransportExpenseService expenseService;
	private final PrincipalAttributeAccessor principalAttributeAccessor;

	@GetMapping
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<List<WmsFulfillmentOrder>> list() {
		return ApiResult.ok(service.listWorkOrders());
	}

	@GetMapping("/page")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<PageResult<FulfillmentShippingOrderVO>> page(PageParam pageParam,
			FulfillmentShippingQuery query) {
		return ApiResult.ok(service.pageWorkOrders(pageParam, query));
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
		return ApiResult.ok(service.ship(ids, principalAttributeAccessor.getUserId()));
	}

	@GetMapping("/handover-orders/page")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<PageResult<OutboundHandoverOrderVO>> handoverPage(PageParam pageParam,
			FulfillmentShippingQuery query) {
		return ApiResult.ok(handoverService.page(pageParam, query));
	}

	@GetMapping("/handover-orders/available-fulfillments")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<List<FulfillmentShippingOrderVO>> availableHandoverFulfillments() {
		return ApiResult.ok(handoverService.availableFulfillments());
	}

	@GetMapping("/handover-orders/recent-destinations")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<List<String>> recentHandoverDestinations() {
		return ApiResult.ok(handoverService.recentDestinations());
	}

	@PostMapping("/handover-orders")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Long> createHandover(@Validated @RequestBody OutboundHandoverCreateDTO dto) {
		return ApiResult.ok(handoverService.create(dto, principalAttributeAccessor.getUserId()));
	}

	@GetMapping("/handover-orders/{id}")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<OutboundHandoverOrderVO> handoverDetail(@PathVariable("id") Long id) {
		return ApiResult.ok(handoverService.detail(id));
	}

	@PutMapping("/handover-orders/{id}")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> saveHandover(@PathVariable("id") Long id,
			@Validated @RequestBody FulfillmentHandoverDTO dto) {
		handoverService.save(id, dto, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@PostMapping("/handover-orders/{id}/confirm")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> confirmHandover(@PathVariable("id") Long id,
			@Validated @RequestBody FulfillmentHandoverDTO dto) {
		handoverService.confirm(id, dto, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@GetMapping("/handover-orders/{id}/pdf")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ResponseEntity<byte[]> handoverPdf(@PathVariable("id") Long id) {
		OutboundHandoverOrderVO order = handoverService.detail(id);
		String encoded;
		try {
			encoded = java.net.URLEncoder.encode("出库交接凭证-" + order.getHandoverNo() + ".pdf", "UTF-8")
					.replace("+", "%20");
		}
		catch (java.io.UnsupportedEncodingException ex) {
			encoded = "handover.pdf";
		}
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
				.contentType(MediaType.APPLICATION_PDF)
				.body(handoverService.buildPdf(id));
	}

	@GetMapping("/expenses")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<List<WmsTransportExpenseRecord>> expenses(@RequestParam(required = false) String type) {
		return ApiResult.ok(expenseService.list(type));
	}

	@PostMapping("/expenses")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Long> createExpense(@Validated @RequestBody TransportExpenseDTO dto) {
		return ApiResult.ok(expenseService.create(dto, principalAttributeAccessor.getUserId()));
	}

	@PutMapping("/expenses/{id}")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> updateExpense(@PathVariable("id") Long id,
			@Validated @RequestBody TransportExpenseDTO dto) {
		expenseService.update(id, dto, principalAttributeAccessor.getUserId());
		return ApiResult.ok();
	}

	@DeleteMapping("/expenses/{id}")
	@PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
	public ApiResult<Void> deleteExpense(@PathVariable("id") Long id) {
		expenseService.delete(id);
		return ApiResult.ok();
	}
}
