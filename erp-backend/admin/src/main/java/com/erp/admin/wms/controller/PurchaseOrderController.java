package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.dto.PurchaseOrderDTO;
import com.erp.admin.wms.model.qo.PurchaseOrderQO;
import com.erp.admin.wms.model.vo.PurchaseOrderDetailVO;
import com.erp.admin.wms.model.vo.PurchaseOrderExportVO;
import com.erp.admin.wms.model.vo.PurchaseOrderPageVO;
import com.erp.admin.wms.facade.PurchaseOrderFacade;
import com.erp.admin.wms.service.PurchaseOrderService;
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
import java.util.List;

/**
 * 采购单管理
 *
 * @author erp
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/wms/purchase-order")
@Tag(name = "采购单管理")
public class PurchaseOrderController {

	private final PurchaseOrderService purchaseOrderService;

	private final PurchaseOrderFacade purchaseOrderFacade;

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询条件
	 * @return 分页结果
	 */
	@Operation(summary = "分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('wms:purchase-order:read')")
	public ApiResult<PageResult<PurchaseOrderPageVO>> getPage(PageParam pageParam, PurchaseOrderQO qo) {
		return ApiResult.ok(purchaseOrderService.queryPage(pageParam, qo));
	}

	/**
	 * 获取详情
	 * @param id 采购单ID
	 * @return 详情
	 */
	@Operation(summary = "获取详情")
	@GetMapping("/detail")
	@PreAuthorize("@per.hasPermission('wms:purchase-order:read')")
	public ApiResult<PurchaseOrderDetailVO> getDetail(@RequestParam Long id) {
		return ApiResult.ok(purchaseOrderService.getDetail(id));
	}

	/**
	 * 新建采购单
	 * @param dto 采购单数据
	 * @return 采购单ID
	 */
	@Operation(summary = "新建采购单")
	@OperationLog(bizType = "采购单管理", successMessage = "新建成功")
	@PostMapping
	@PreAuthorize("@per.hasPermission('wms:purchase-order:add')")
	public ApiResult<Long> create(@Validated({Default.class, CreateGroup.class}) @RequestBody PurchaseOrderDTO dto) {
		Long id = purchaseOrderFacade.create(dto);
		return ApiResult.ok(id);
	}

	/**
	 * 编辑采购单
	 * @param dto 采购单数据
	 * @return 操作结果
	 */
	@Operation(summary = "编辑采购单")
	@OperationLog(bizType = "采购单管理", successMessage = "编辑成功")
	@PutMapping
	@PreAuthorize("@per.hasPermission('wms:purchase-order:edit')")
	public ApiResult<Void> update(@Validated({Default.class, UpdateGroup.class}) @RequestBody PurchaseOrderDTO dto) {
		purchaseOrderFacade.update(dto);
		return ApiResult.ok();
	}

	/**
	 * 删除采购单
	 * @param id 采购单ID
	 * @return 操作结果
	 */
	@Operation(summary = "删除采购单")
	@OperationLog(bizType = "采购单管理", successMessage = "删除成功")
	@DeleteMapping
	@PreAuthorize("@per.hasPermission('wms:purchase-order:del')")
	public ApiResult<Void> delete(@RequestParam Long id) {
		purchaseOrderFacade.delete(id);
		return ApiResult.ok();
	}

	/**
	 * 确认采购单
	 * @param id 采购单ID
	 * @return 操作结果
	 */
	@Operation(summary = "确认采购单")
	@OperationLog(bizType = "采购单管理", successMessage = "确认成功")
	@PatchMapping("/confirm")
	@PreAuthorize("@per.hasPermission('wms:purchase-order:edit')")
	public ApiResult<Void> confirm(@RequestParam Long id) {
		purchaseOrderFacade.confirm(id);
		return ApiResult.ok();
	}

	/**
	 * 开始生产
	 * @param id 采购单ID
	 * @return 操作结果
	 */
	@Operation(summary = "开始生产")
	@OperationLog(bizType = "采购单管理", successMessage = "开始生产成功")
	@PatchMapping("/start-production")
	@PreAuthorize("@per.hasPermission('wms:purchase-order:edit')")
	public ApiResult<Void> startProduction(@RequestParam Long id) {
		purchaseOrderFacade.startProduction(id);
		return ApiResult.ok();
	}

	/**
	 * 取消采购单
	 * @param id 采购单ID
	 * @return 操作结果
	 */
	@Operation(summary = "取消采购单")
	@OperationLog(bizType = "采购单管理", successMessage = "取消成功")
	@PatchMapping("/cancel")
	@PreAuthorize("@per.hasPermission('wms:purchase-order:edit')")
	public ApiResult<Void> cancel(@RequestParam Long id) {
		purchaseOrderFacade.cancel(id);
		return ApiResult.ok();
	}

	/**
	 * 导出采购单
	 * @param qo 查询条件
	 * @return 导出数据列表
	 */
	@Operation(summary = "导出采购单")
	@GetMapping("/export")
	@AccessLoggingRule(includeQueryString = true, includeRequestBody = true)
	@ResponseExcel(name = "采购单-#{currentDateTime()}")
	@PreAuthorize("@per.hasPermission('wms:purchase-order:read')")
	public List<PurchaseOrderExportVO> export(PurchaseOrderQO qo) {
		return purchaseOrderService.listForExport(qo);
	}

}
