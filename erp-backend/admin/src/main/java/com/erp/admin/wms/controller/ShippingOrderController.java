package com.erp.admin.wms.controller;

import java.util.List;

import com.erp.admin.wms.facade.ShippingOrderFacade;
import com.erp.admin.wms.model.dto.ShippingOrderDTO;
import com.erp.admin.wms.model.qo.AvailableItemQO;
import com.erp.admin.wms.model.qo.ShippingOrderQO;
import com.erp.admin.wms.model.vo.AvailableItemVO;
import com.erp.admin.wms.model.vo.PurchaseInboundSimpleVO;
import com.erp.admin.wms.model.vo.ShippingOrderDetailVO;
import com.erp.admin.wms.model.vo.ShippingOrderExportVO;
import com.erp.admin.wms.model.vo.ShippingOrderPageVO;
import com.erp.admin.wms.model.vo.ShippingOrderSimpleVO;
import com.erp.admin.wms.service.PurchaseInboundService;
import com.erp.admin.wms.service.ShippingOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.validation.group.CreateGroup;
import org.ballcat.common.core.validation.group.UpdateGroup;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.fastexcel.annotation.ResponseExcel;
import org.ballcat.log.operation.annotation.OperationLog;
import org.ballcat.web.accesslog.annotation.AccessLoggingRule;
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

/**
 * 物流单管理
 *
 * @author erp
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/shipping-order")
@Tag(name = "物流单管理")
public class ShippingOrderController {

	private final ShippingOrderService shippingOrderService;

	private final ShippingOrderFacade shippingOrderFacade;

	private final PurchaseInboundService purchaseInboundService;

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 物流单查询对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('wms:shipping-order:read')")
	public ApiResult<PageResult<ShippingOrderPageVO>> getPage(PageParam pageParam, ShippingOrderQO qo) {
		return ApiResult.ok(this.shippingOrderService.queryPage(pageParam, qo));
	}

	/**
	 * 物流单详情
	 * @param id 物流单ID
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "物流单详情")
	@GetMapping("/detail")
	@PreAuthorize("@per.hasPermission('wms:shipping-order:read')")
	public ApiResult<ShippingOrderDetailVO> getDetail(@RequestParam Long id) {
		return ApiResult.ok(this.shippingOrderService.getDetail(id));
	}

	/**
	 * 新建物流单
	 * @param dto 物流单数据传输对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "新建物流单")
	@OperationLog(bizType = "物流单管理", successMessage = "新建成功")
	@PostMapping
	@PreAuthorize("@per.hasPermission('wms:shipping-order:add')")
	public ApiResult<Long> create(@Validated({Default.class, CreateGroup.class}) @RequestBody ShippingOrderDTO dto) {
		Long id = this.shippingOrderFacade.create(dto);
		return ApiResult.ok(id);
	}

	/**
	 * 编辑物流单
	 * @param dto 物流单数据传输对象
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "编辑物流单")
	@OperationLog(bizType = "物流单管理", successMessage = "编辑成功")
	@PutMapping
	@PreAuthorize("@per.hasPermission('wms:shipping-order:edit')")
	public ApiResult<Void> update(@Validated({Default.class, UpdateGroup.class}) @RequestBody ShippingOrderDTO dto) {
		this.shippingOrderFacade.update(dto);
		return ApiResult.ok();
	}

	/**
	 * 确认发货
	 * @param id 物流单ID
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "确认发货")
	@OperationLog(bizType = "物流单管理", successMessage = "确认发货成功")
	@PatchMapping("/ship")
	@PreAuthorize("@per.hasPermission('wms:shipping-order:edit')")
	public ApiResult<Void> confirmShip(@RequestParam Long id) {
		this.shippingOrderFacade.confirmShip(id);
		return ApiResult.ok();
	}

	/**
	 * 删除物流单
	 * @param ids 物流单ID列表
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "删除物流单")
	@OperationLog(bizType = "物流单管理", successMessage = "删除成功")
	@DeleteMapping
	@PreAuthorize("@per.hasPermission('wms:shipping-order:del')")
	public ApiResult<Void> delete(@RequestBody List<Long> ids) {
		this.shippingOrderFacade.delete(ids);
		return ApiResult.ok();
	}

	/**
	 * 更新付款状态
	 * @param id 物流单ID
	 * @param paymentStatus 付款状态: 0-未付 / 1-已付
	 * @param voucherFileId 付款凭证文件ID（paymentStatus=1时必传）
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "更新付款状态")
	@OperationLog(bizType = "物流单管理", successMessage = "付款状态更新成功")
	@PatchMapping("/payment")
	@PreAuthorize("@per.hasPermission('wms:shipping-order:edit')")
	public ApiResult<Void> updatePaymentStatus(
			@RequestParam Long id,
			@RequestParam Integer paymentStatus,
			@RequestParam(required = false) Long voucherFileId) {
		this.shippingOrderService.updatePaymentStatus(id, paymentStatus, voucherFileId);
		return ApiResult.ok();
	}

	/**
	 * 获取可发货采购明细
	 * @param qo 查询条件
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "获取可发货采购明细")
	@GetMapping("/available-items")
	@PreAuthorize("@per.hasPermission('wms:shipping-order:read')")
	public ApiResult<List<AvailableItemVO>> getAvailableItems(AvailableItemQO qo) {
		return ApiResult.ok(this.shippingOrderService.getAvailableItems(qo));
	}

	/**
	 * 导出物流单
	 * @param qo 查询条件
	 * @return 导出数据列表
	 */
	@Operation(summary = "导出物流单")
	@GetMapping("/export")
	@AccessLoggingRule(includeQueryString = true, includeRequestBody = true)
	@ResponseExcel(name = "物流单-#{currentDateTime()}")
	@PreAuthorize("@per.hasPermission('wms:shipping-order:read')")
	public List<ShippingOrderExportVO> export(ShippingOrderQO qo) {
		return this.shippingOrderService.listForExport(qo);
	}

	/**
	 * 根据采购单ID查询关联物流单
	 * @param purchaseOrderId 采购单ID
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "根据采购单ID查询关联物流单")
	@GetMapping("/by-purchase-order")
	@PreAuthorize("@per.hasPermission('wms:shipping-order:read')")
	public ApiResult<List<ShippingOrderSimpleVO>> getByPurchaseOrderId(@RequestParam Long purchaseOrderId) {
		return ApiResult.ok(this.shippingOrderService.getByPurchaseOrderId(purchaseOrderId));
	}

	/**
	 * 查询关联入库单
	 * @param shippingOrderId 物流单ID
	 * @return ApiResult 通用返回体
	 */
	@Operation(summary = "查询关联入库单")
	@GetMapping("/related-inbounds")
	@PreAuthorize("@per.hasPermission('wms:shipping-order:read')")
	public ApiResult<List<PurchaseInboundSimpleVO>> getRelatedInbounds(@RequestParam Long shippingOrderId) {
		// Controller 层直接调用 PurchaseInboundService，避免 Service 层循环依赖
		return ApiResult.ok(this.purchaseInboundService.getByShippingOrderId(shippingOrderId));
	}

}
