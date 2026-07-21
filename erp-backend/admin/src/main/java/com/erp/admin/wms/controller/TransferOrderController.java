package com.erp.admin.wms.controller;

import com.erp.admin.wms.facade.TransferOrderFacade;
import com.erp.admin.wms.model.dto.BatchStockQueryDTO;
import com.erp.admin.wms.model.dto.TransferOrderDTO;
import com.erp.admin.wms.model.qo.TransferOrderQO;
import com.erp.admin.wms.model.vo.AvailableStockVO;
import com.erp.admin.wms.model.vo.TransferOrderDetailVO;
import com.erp.admin.wms.model.vo.TransferOrderPageVO;
import com.erp.admin.wms.service.TransferOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.validation.group.CreateGroup;
import org.ballcat.common.core.validation.group.UpdateGroup;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.groups.Default;
import java.util.List;
import java.util.Map;

/**
 * 调拨单管理
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/transfer-order")
@Tag(name = "调拨单管理")
public class TransferOrderController {

	private final TransferOrderService transferOrderService;
	private final TransferOrderFacade transferOrderFacade;

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询条件
	 * @return 分页结果
	 */
	@Operation(summary = "分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('wms:transfer-order:read')")
	public ApiResult<PageResult<TransferOrderPageVO>> getPage(PageParam pageParam, TransferOrderQO qo) {
		return ApiResult.ok(transferOrderService.queryPage(pageParam, qo));
	}

	/**
	 * 获取详情
	 * @param id 调拨单ID
	 * @return 调拨单详情
	 */
	@Operation(summary = "获取详情")
	@GetMapping("/detail")
	@PreAuthorize("@per.hasPermission('wms:transfer-order:read')")
	public ApiResult<TransferOrderDetailVO> getDetail(@RequestParam Long id) {
		return ApiResult.ok(transferOrderService.getDetail(id));
	}

	/**
	 * 新建调拨单
	 * @param dto 调拨单DTO
	 * @return 调拨单ID
	 */
	@Operation(summary = "新建调拨单")
	@OperationLog(bizType = "调拨单管理", successMessage = "新建成功")
	@PostMapping
	@PreAuthorize("@per.hasPermission('wms:transfer-order:add')")
	public ApiResult<Long> create(@Validated({Default.class, CreateGroup.class}) @RequestBody TransferOrderDTO dto) {
		return ApiResult.ok(transferOrderService.create(dto));
	}

	/**
	 * 编辑调拨单
	 * @param dto 调拨单DTO
	 * @return 操作结果
	 */
	@Operation(summary = "编辑调拨单")
	@OperationLog(bizType = "调拨单管理", successMessage = "编辑成功")
	@PutMapping
	@PreAuthorize("@per.hasPermission('wms:transfer-order:edit')")
	public ApiResult<Void> update(@Validated({Default.class, UpdateGroup.class}) @RequestBody TransferOrderDTO dto) {
		transferOrderService.update(dto);
		return ApiResult.ok();
	}

	/**
	 * 确认出库
	 * @param id 调拨单ID
	 * @return 操作结果
	 */
	@Operation(summary = "确认出库")
	@OperationLog(bizType = "调拨单管理", successMessage = "确认出库成功")
	@PatchMapping("/ship")
	@PreAuthorize("@per.hasPermission('wms:transfer-order:ship')")
	public ApiResult<Void> ship(@RequestParam Long id) {
		transferOrderFacade.confirmShipment(id);
		return ApiResult.ok();
	}

	/**
	 * 确认入库
	 * @param id 调拨单ID
	 * @return 操作结果
	 */
	@Operation(summary = "确认入库")
	@OperationLog(bizType = "调拨单管理", successMessage = "确认入库成功")
	@PatchMapping("/receive")
	@PreAuthorize("@per.hasPermission('wms:transfer-order:receive')")
	public ApiResult<Void> receive(@RequestParam Long id) {
		transferOrderFacade.confirmReceipt(id);
		return ApiResult.ok();
	}

	/**
	 * 取消调拨单
	 * @param id 调拨单ID
	 * @return 操作结果
	 */
	@Operation(summary = "取消调拨单")
	@OperationLog(bizType = "调拨单管理", successMessage = "取消成功")
	@PatchMapping("/cancel")
	@PreAuthorize("@per.hasPermission('wms:transfer-order:cancel')")
	public ApiResult<Void> cancel(@RequestParam Long id) {
		transferOrderFacade.cancel(id);
		return ApiResult.ok();
	}

	/**
	 * 撤回调拨单
	 * @param id 调拨单ID
	 * @return 操作结果
	 */
	@Operation(summary = "撤回调拨单")
	@OperationLog(bizType = "调拨单管理", successMessage = "撤回成功")
	@PatchMapping("/revoke")
	@PreAuthorize("@per.hasPermission('wms:transfer-order:revoke')")
	public ApiResult<Void> revoke(@RequestParam Long id) {
		transferOrderFacade.revoke(id);
		return ApiResult.ok();
	}

	/**
	 * 删除调拨单
	 * @param id 调拨单ID
	 * @return 操作结果
	 */
	@Operation(summary = "删除调拨单")
	@OperationLog(bizType = "调拨单管理", successMessage = "删除成功")
	@DeleteMapping
	@PreAuthorize("@per.hasPermission('wms:transfer-order:del')")
	public ApiResult<Void> delete(@RequestParam Long id) {
		transferOrderService.delete(id);
		return ApiResult.ok();
	}

	/**
	 * 分页查询可调拨库存
	 * @param pageParam 分页参数
	 * @param warehouseId 仓库ID
	 * @param keyword 关键字（SKU编码或名称）
	 * @return 分页结果
	 */
	@Operation(summary = "分页查询可调拨库存")
	@GetMapping("/available-stock/page")
	@PreAuthorize("@per.hasPermission('wms:transfer-order:read')")
	public ApiResult<PageResult<AvailableStockVO>> pageAvailableStock(PageParam pageParam,
			@RequestParam Long warehouseId,
			@RequestParam(required = false) String keyword) {
		return ApiResult.ok(transferOrderService.pageAvailableStock(pageParam, warehouseId, keyword));
	}

	/**
	 * 批量查询可用库存
	 * @param dto 批量查询参数
	 * @return skuCode -> availableQuantity 映射
	 */
	@Operation(summary = "批量查询可用库存")
	@PostMapping("/available-stock/batch")
	@PreAuthorize("@per.hasPermission('wms:transfer-order:read')")
	public ApiResult<Map<String, Integer>> batchQueryStock(@Validated @RequestBody BatchStockQueryDTO dto) {
		return ApiResult.ok(transferOrderService.batchQueryStock(dto.getWarehouseId(), dto.getSkuCodes()));
	}

	@Operation(summary = "源批次候选（A仓·某货主可调拨批次）")
	@GetMapping("/source-batches")
	@PreAuthorize("@per.hasPermission('wms:transfer-order:read')")
	public ApiResult<List<com.erp.admin.wms.model.entity.WmsPhysicalInventory>> sourceBatches(
			@RequestParam Long fromWarehouseId, @RequestParam Long erpTenantId,
			@RequestParam(required = false) String skuKeyword) {
		return ApiResult.ok(transferOrderService.listSourceBatches(fromWarehouseId, erpTenantId, skuKeyword));
	}

	@Operation(summary = "目标库位候选（B仓·该货主服务商租用标准库位）")
	@GetMapping("/target-locations")
	@PreAuthorize("@per.hasPermission('wms:transfer-order:read')")
	public ApiResult<List<com.erp.admin.wms.model.vo.AvailableLocationVO>> targetLocations(
			@RequestParam Long toWarehouseId, @RequestParam Long erpTenantId) {
		return ApiResult.ok(transferOrderService.listTargetLocations(toWarehouseId, erpTenantId));
	}

}
