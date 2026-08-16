package com.erp.admin.wms.controller;

import java.util.List;

import com.erp.admin.wms.model.dto.ManualFulfillmentDTO;
import com.erp.admin.wms.model.entity.WmsFulfillmentItem;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.service.ManualFulfillmentService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/manual-fulfillment")
@RequiredArgsConstructor
public class ManualFulfillmentController {
	private final ManualFulfillmentService service;

	@GetMapping
	@PreAuthorize("@per.hasPermission('wms:custom-outbound:read')")
	public ApiResult<List<WmsFulfillmentOrder>> list() { return ApiResult.ok(service.list()); }

	@GetMapping("/{id}")
	@PreAuthorize("@per.hasPermission('wms:custom-outbound:read')")
	public ApiResult<WmsFulfillmentOrder> detail(@PathVariable Long id) { return ApiResult.ok(service.detail(id)); }

	@GetMapping("/{id}/items")
	@PreAuthorize("@per.hasPermission('wms:custom-outbound:read')")
	public ApiResult<List<WmsFulfillmentItem>> items(@PathVariable Long id) { return ApiResult.ok(service.listItems(id)); }

	@PostMapping
	@PreAuthorize("@per.hasPermission('wms:custom-outbound:add')")
	public ApiResult<Long> create(@Validated @RequestBody ManualFulfillmentDTO dto) {
		dto.setId(null);
		return ApiResult.ok(service.saveDraft(dto));
	}

	@PutMapping("/{id}")
	@PreAuthorize("@per.hasPermission('wms:custom-outbound:edit')")
	public ApiResult<Long> update(@PathVariable Long id, @Validated @RequestBody ManualFulfillmentDTO dto) {
		dto.setId(id);
		return ApiResult.ok(service.saveDraft(dto));
	}

	@PostMapping("/{id}/submit")
	@PreAuthorize("@per.hasPermission('wms:custom-outbound:edit')")
	public ApiResult<Void> submit(@PathVariable Long id) { service.submit(id); return ApiResult.ok(); }

	@DeleteMapping("/{id}")
	@PreAuthorize("@per.hasPermission('wms:custom-outbound:del')")
	public ApiResult<Void> delete(@PathVariable Long id) { service.deleteDraft(id); return ApiResult.ok(); }
}
