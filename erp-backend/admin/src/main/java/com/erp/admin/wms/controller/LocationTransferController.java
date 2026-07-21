package com.erp.admin.wms.controller;

import java.util.List;

import com.erp.admin.wms.model.dto.LocationTransferCreateDTO;
import com.erp.admin.wms.model.qo.LocationTransferQO;
import com.erp.admin.wms.model.vo.AvailableLocationVO;
import com.erp.admin.wms.model.vo.LocationTransferDetailVO;
import com.erp.admin.wms.model.vo.LocationTransferPageVO;
import com.erp.admin.wms.service.LocationTransferOrderService;
import com.erp.admin.wms.service.LocationTransferService;
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

/**
 * 库位调整单（库内移库单）。平台新建调整单→执行「调整完成」逐条移库→留痕。
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/location-transfer")
@Tag(name = "库位调整单")
public class LocationTransferController {

	private final LocationTransferOrderService orderService;

	private final LocationTransferService locationTransferService;

	@Operation(summary = "库位调整单分页")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('wms:location:read')")
	public ApiResult<PageResult<LocationTransferPageVO>> page(PageParam pageParam, LocationTransferQO qo) {
		return ApiResult.ok(orderService.queryPage(pageParam, qo));
	}

	@Operation(summary = "库位调整单详情")
	@GetMapping("/detail")
	@PreAuthorize("@per.hasPermission('wms:location:read')")
	public ApiResult<LocationTransferDetailVO> detail(@RequestParam("id") Long id) {
		return ApiResult.ok(orderService.getDetail(id));
	}

	@Operation(summary = "新建库位调整单")
	@OperationLog(bizType = "库位调整", successMessage = "新建库位调整单成功")
	@PostMapping
	@PreAuthorize("@per.hasPermission('wms:location:transfer')")
	public ApiResult<Long> create(@Validated @RequestBody LocationTransferCreateDTO dto) {
		return ApiResult.ok(orderService.create(dto));
	}

	@Operation(summary = "调整完成（执行移库）")
	@OperationLog(bizType = "库位调整", successMessage = "库位调整完成")
	@PatchMapping("/complete")
	@PreAuthorize("@per.hasPermission('wms:location:transfer')")
	public ApiResult<Void> complete(@RequestParam("id") Long id) {
		orderService.complete(id);
		return ApiResult.ok();
	}

	@Operation(summary = "撤销库位调整单")
	@OperationLog(bizType = "库位调整", successMessage = "库位调整单已撤销")
	@PatchMapping("/cancel")
	@PreAuthorize("@per.hasPermission('wms:location:transfer')")
	public ApiResult<Void> cancel(@RequestParam("id") Long id) {
		orderService.cancel(id);
		return ApiResult.ok();
	}

	@Operation(summary = "删除库位调整单（仅已取消）")
	@OperationLog(bizType = "库位调整", successMessage = "库位调整单已删除")
	@DeleteMapping
	@PreAuthorize("@per.hasPermission('wms:location:transfer')")
	public ApiResult<Void> delete(@RequestBody List<Long> ids) {
		orderService.delete(ids);
		return ApiResult.ok();
	}

	@Operation(summary = "目标库位候选")
	@GetMapping("/candidates")
	@PreAuthorize("@per.hasPermission('wms:location:read')")
	public ApiResult<List<AvailableLocationVO>> candidates(@RequestParam("physicalInventoryId") Long physicalInventoryId) {
		return ApiResult.ok(locationTransferService.listCandidateTargets(physicalInventoryId));
	}

}
