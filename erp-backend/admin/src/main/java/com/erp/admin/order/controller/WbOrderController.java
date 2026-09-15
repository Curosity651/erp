package com.erp.admin.order.controller;

import java.util.List;
import java.util.function.BiFunction;

import com.erp.admin.order.model.qo.ErpOrderQO;
import com.erp.admin.order.model.vo.SyncSummaryVO;
import com.erp.admin.order.model.vo.WbOrderExportVO;
import com.erp.admin.order.model.vo.WbOrderPageVO;
import com.erp.admin.order.service.ErpOrderService;
import com.erp.admin.order.service.wildberries.WbOrderQueryService;
import com.erp.admin.order.service.wildberries.WbOrderSyncService;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.product.excel.ImageAwareDataFetcher;
import com.erp.admin.product.excel.ImageByteFetcher;
import okhttp3.OkHttpClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.common.model.result.BaseResultCode;
import org.ballcat.fastexcel.annotation.ResponseExcel;
import org.ballcat.fastexcel.handler.DataFetchIterableSheetDataProvider;
import org.ballcat.fastexcel.handler.PageDataFetcher;
import org.ballcat.web.accesslog.annotation.AccessLoggingRule;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 订单主表
 *
 * @author erp 2025-09-27 23:16:08
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order/wb-order")
@Tag(name = "Wb订单管理")
public class WbOrderController {

	private final WbOrderQueryService wbOrderQueryService;
	private final ErpOrderService erpOrderService;
	private final WbOrderSyncService wbOrderSyncService;
	private final OkHttpClient okHttpClient;


	/**
	 * 分页查询 Wildberries 订单
	 *
	 * @param pageParam  分页参数
	 * @param erpOrderQO 订单主表查询对象
	 * @return ApiResult 通用返回体（包含完整的办公室信息）
	 */
	@Operation(summary = "分页查询 Wildberries 订单")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('order:erp-order:read')")
	public ApiResult<PageResult<WbOrderPageVO>> getErpOrderPage(
			PageParam pageParam, ErpOrderQO erpOrderQO) {
		// 强制设置平台为 wildberries
		erpOrderQO.setPlatform(PlatformEnum.Wildberries.code());
		return ApiResult.ok(wbOrderQueryService.queryPage(pageParam, erpOrderQO));
	}


	/**
	 * 设置锁定
	 */
	@Operation(summary = "锁定订单")
	@PatchMapping("/{id}/lock")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	public ApiResult<Void> lock(@PathVariable Long id) {
		return erpOrderService.setLocked(id, 1) ? ApiResult.ok() : ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR);
	}

	/**
	 * 取消锁定
	 */
	@Operation(summary = "解锁订单")
	@PatchMapping("/{id}/unlock")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	public ApiResult<Void> unlock(@PathVariable Long id) {
		return erpOrderService.setLocked(id, 0) ? ApiResult.ok() : ApiResult.failed(BaseResultCode.UPDATE_DATABASE_ERROR);
	}

	/**
	 * 同步订单状态以及（可选）关联 supply 信息，仅支持 Wildberries 平台。
	 * 支持单条/批量：前端统一传入订单ID列表。
	 */
	@Operation(summary = "同步订单（仅WB），入参支持单条/批量")
	@PostMapping(value = "/sync")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	public ApiResult<SyncSummaryVO> syncOrders(@RequestBody List<Long> orderIds) {
		SyncSummaryVO vo = wbOrderSyncService.syncOrdersByIds(orderIds);
		return ApiResult.ok(vo);
	}

	/**
	 * 同步全量订单信息，仅支持 Wildberries 平台。
	 */
	@Operation(summary = "同步全量 WB 订单")
	@PostMapping(value = "/sync-all")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	public ApiResult<Void> syncAllOrders() {
		wbOrderSyncService.syncAllEnabledShopOrders(true);
		return ApiResult.ok();
	}

	/**
	 * 导出 Wildberries 订单
	 * <p>
	 * 使用分批流式导出，避免大数据量导致内存溢出。
	 * 图片使用多线程预下载优化，提升导出性能。
	 *
	 * @param erpOrderQO 查询条件
	 * @return 可迭代的数据提供者
	 */
	@Operation(summary = "导出 Wildberries 订单")
	@PostMapping(value = "/export")
	@AccessLoggingRule(includeQueryString = true, includeRequestBody = true)
	@ResponseExcel(name = "Wildberries订单导出_#{currentDateTime()}")
	@PreAuthorize("@per.hasPermission('order:erp-order:read')")
	public DataFetchIterableSheetDataProvider<WbOrderExportVO> exportOrders(@RequestBody ErpOrderQO erpOrderQO) {
		// 强制设置平台
		erpOrderQO.setPlatform(PlatformEnum.Wildberries.code());

		// 每批 100 条，减少内存占用
		final int batchSize = 100;

		// 创建分页数据获取函数
		BiFunction<Integer, Integer, List<WbOrderExportVO>> pageFetcher =
				(currentPage, pageSize) -> wbOrderQueryService.queryExportBatch(erpOrderQO, currentPage, pageSize);

		// 创建分页数据获取器
		PageDataFetcher<WbOrderExportVO> pageDataFetcher = new PageDataFetcher<>(pageFetcher, batchSize);

		// 使用图片感知数据获取器包装，实现图片多线程下载并填充到 VO
		ImageByteFetcher imageFetcher = new ImageByteFetcher(okHttpClient);
		ImageAwareDataFetcher<WbOrderExportVO> dataFetcher = new ImageAwareDataFetcher<>(
				pageDataFetcher,
				imageFetcher
		);

		// 创建可迭代数据提供者
		return new DataFetchIterableSheetDataProvider<>(WbOrderExportVO.class, dataFetcher);
	}

}
