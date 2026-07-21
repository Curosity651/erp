package com.erp.admin.order.controller;

import java.util.List;

import com.erp.admin.order.model.qo.ErpOrderQO;
import com.erp.admin.order.model.vo.LabelBatchVO;
import com.erp.admin.order.model.vo.SyncSummaryVO;
import com.erp.admin.order.model.vo.YdOrderPageVO;
import com.erp.admin.order.service.ErpOrderService;
import com.erp.admin.order.service.label.LabelPrintOrchestrator;
import com.erp.admin.order.service.yandex.YdOrderConfirmService;
import com.erp.admin.order.service.yandex.YdOrderQueryService;
import com.erp.admin.order.service.yandex.YdOrderSyncService;
import com.erp.admin.order.service.common.model.ConfirmResult;
import com.erp.admin.platform.PlatformEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Yandex 订单管理
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order/yd-order")
@Tag(name = "Yandex 订单管理")
@Slf4j
public class YdOrderController {

	private final YdOrderQueryService ydOrderQueryService;
	private final YdOrderSyncService ydOrderSyncService;
	private final YdOrderConfirmService ydOrderConfirmService;
	private final ErpOrderService erpOrderService;
	private final LabelPrintOrchestrator labelPrintOrchestrator;
	private final PrincipalAttributeAccessor principalAttributeAccessor;

	/**
	 * 分页查询 Yandex 订单
	 */
	@Operation(summary = "分页查询 Yandex 订单")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('order:erp-order:read')")
	public ApiResult<PageResult<YdOrderPageVO>> getPage(
			PageParam pageParam, ErpOrderQO qo) {
		qo.setPlatform(PlatformEnum.Yandex.code());
		return ApiResult.ok(ydOrderQueryService.queryPage(pageParam, qo));
	}

	/**
	 * 批量确认发货
	 */
	@PostMapping("/confirm")
	@Operation(summary = "批量确认发货")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	public ApiResult<ConfirmResult> confirm(@RequestBody List<Long> orderIds) {
		log.info("[YANDEX][CONTROLLER] 批量确认发货: orderIds={}", orderIds);
		ConfirmResult result = ydOrderConfirmService.confirmOrders(orderIds);
		return ApiResult.ok(result);
	}

	/**
	 * 批量打印面单
	 */
	@PostMapping("/print-labels")
	@Operation(summary = "批量打印面单")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	public ApiResult<LabelBatchVO> printLabels(@RequestBody List<Long> orderIds) {
		log.info("[YANDEX][CONTROLLER] 批量打印面单: orderIds={}", orderIds);
		Long currentUserId = principalAttributeAccessor.getUserId();
		LabelBatchVO result = labelPrintOrchestrator.printLabels(
				PlatformEnum.Yandex.code(), orderIds, currentUserId, "批量打印");
		return ApiResult.ok(result);
	}

	/**
	 * 锁定订单
	 */
	@PostMapping("/lock/{id}")
	@Operation(summary = "锁定订单")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	public ApiResult<Boolean> lock(@PathVariable Long id) {
		log.info("[YANDEX][CONTROLLER] 锁定订单: id={}", id);
		boolean result = erpOrderService.setLocked(id, 1);
		return ApiResult.ok(result);
	}

	/**
	 * 解锁订单
	 */
	@PostMapping("/unlock/{id}")
	@Operation(summary = "解锁订单")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	public ApiResult<Boolean> unlock(@PathVariable Long id) {
		log.info("[YANDEX][CONTROLLER] 解锁订单: id={}", id);
		boolean result = erpOrderService.setLocked(id, 0);
		return ApiResult.ok(result);
	}

	/**
	 * 按订单 ID 同步
	 */
	@Operation(summary = "按订单 ID 同步 Yandex 订单")
	@PostMapping("/sync")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	public ApiResult<SyncSummaryVO> syncOrders(@RequestBody List<Long> orderIds) {
		log.info("[YANDEX][CONTROLLER] 按 ID 同步订单: orderIds={}", orderIds);
		SyncSummaryVO result = ydOrderSyncService.syncOrdersByIds(orderIds);
		return ApiResult.ok(result);
	}

	/**
	 * 全量同步
	 */
	@Operation(summary = "全量同步 Yandex 订单")
	@PostMapping("/sync-all")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	public ApiResult<Void> syncAll() {
		ydOrderSyncService.syncAllShops(true);
		return ApiResult.ok();
	}
}
