package com.erp.admin.order.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;

import com.erp.admin.order.model.qo.ErpOrderQO;
import com.erp.admin.order.model.dto.ozon.OzonActCreateDTO;
import com.erp.admin.order.model.vo.LabelBatchVO;
import com.erp.admin.order.model.vo.OzonActBatchVO;
import com.erp.admin.order.model.vo.OzonOrderExportVO;
import com.erp.admin.order.model.vo.OzonOrderPageVO;
import com.erp.admin.order.model.vo.OzonPickListBatchVO;
import com.erp.admin.order.model.vo.SyncSummaryVO;
import com.erp.admin.order.service.ozon.OzonActService;
import com.erp.admin.order.service.ozon.OzonPickListService;
import com.erp.admin.order.service.label.LabelPrintOrchestrator;
import com.erp.admin.order.service.ErpOrderService;
import com.erp.admin.order.service.ozon.OzonOrderConfirmService;
import com.erp.admin.order.service.ozon.OzonOrderQueryService;
import com.erp.admin.order.service.ozon.OzonOrderSyncService;
import com.erp.admin.order.service.common.model.ConfirmResult;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.product.excel.ImageAwareDataFetcher;
import com.erp.admin.product.excel.ImageByteFetcher;
import okhttp3.OkHttpClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.fastexcel.annotation.ResponseExcel;
import org.ballcat.fastexcel.handler.DataFetchIterableSheetDataProvider;
import org.ballcat.fastexcel.handler.PageDataFetcher;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.ballcat.web.accesslog.annotation.AccessLoggingRule;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OzonOrderController - Ozon 订单管理控制器
 * <p>
 * 职责：
 * - 处理 Ozon 订单相关的 HTTP 请求
 * - 权限控制
 * - 参数验证
 * - 调用 OzonOrderQueryService / OzonOrderConfirmService / OzonOrderSyncService
 * <p>
 * 注意：
 * - 强制设置 platform='ozon'
 * - 与 WbOrderController 分离，便于权限管理
 *
 * @author system
 */
@RestController
@RequestMapping("/order/ozon-order")
@Tag(name = "Ozon 订单管理")
@RequiredArgsConstructor
@Slf4j
public class OzonOrderController {

	private final OzonOrderQueryService ozonOrderQueryService;
	private final OzonActService ozonActService;
	private final OzonPickListService ozonPickListService;
	private final OzonOrderConfirmService ozonOrderConfirmService;
	private final ErpOrderService erpOrderService;
	private final OzonOrderSyncService ozonOrderSyncService;
	private final LabelPrintOrchestrator labelPrintOrchestrator;
	private final PrincipalAttributeAccessor principalAttributeAccessor;
	private final OkHttpClient okHttpClient;


	/**
	 * 分页查询 Ozon 订单
	 *
	 * @param pageParam 分页参数
	 * @param qo        查询条件
	 * @return 分页结果（包含完整的仓库、物流信息）
	 */
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('order:erp-order:read')")
	@Operation(summary = "分页查询 Ozon 订单")
	public ApiResult<PageResult<OzonOrderPageVO>> page(
			PageParam pageParam,
			ErpOrderQO qo) {
		// 强制设置平台为 ozon（Service 层也会设置，这里明确一下）
		qo.setPlatform(PlatformEnum.Ozon.code());
		PageResult<OzonOrderPageVO> result = ozonOrderQueryService.queryPage(pageParam, qo);
		return ApiResult.ok(result);
	}

	/**
	 * 批量确认发货
	 * <p>
	 * 注意：
	 * - 只处理 FBS 订单
	 * - FBO 订单会被跳过
	 *
	 * @param orderIds 订单ID列表
	 * @return 确认结果
	 */
	@PostMapping("/confirm")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	@Operation(summary = "批量确认发货")
	public ApiResult<ConfirmResult> confirm(@RequestBody List<Long> orderIds) {
		log.info("[OZON][CONTROLLER] 批量确认发货: orderIds={}", orderIds);
		ConfirmResult result = ozonOrderConfirmService.confirmOrders(orderIds);
		return ApiResult.ok(result);
	}

	/**
	 * 同步订单状态
	 * <p>
	 * 从 Ozon 平台批量同步订单的最新状态
	 *
	 * @param orderIds 订单ID列表
	 * @return 同步结果摘要
	 */
	@PostMapping("/sync")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	@Operation(summary = "同步订单状态")
	public ApiResult<SyncSummaryVO> sync(@RequestBody List<Long> orderIds) {
		log.info("[OZON][CONTROLLER] 同步订单状态: orderIds={}", orderIds);
		SyncSummaryVO result = ozonOrderSyncService.syncOrdersByIds(orderIds);
		return ApiResult.ok(result);
	}

	/**
	 * 同步店铺全量订单
	 * <p>
	 * 同步指定店铺最近 30 天的所有订单（FBS + FBO）
	 *
	 * @param shopId 店铺ID
	 * @return 成功响应
	 */
	@PostMapping("/sync-shop/{shopId}")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	@Operation(summary = "同步店铺全量订单")
	public ApiResult<Void> syncShop(@PathVariable Long shopId) {
		log.info("[OZON][CONTROLLER] 同步店铺全量订单: shopId={}", shopId);

		LocalDateTime since = LocalDateTime.now().minusDays(30);
		LocalDateTime to = LocalDateTime.now();

		ozonOrderSyncService.syncShopPostings(shopId, since, to);

		return ApiResult.ok();
	}

	/**
	 * 锁定订单
	 * <p>
	 * 锁定后的订单不能进行确认发货等操作
	 *
	 * @param id 订单ID
	 * @return 是否成功
	 */
	@PostMapping("/lock/{id}")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	@Operation(summary = "锁定订单")
	public ApiResult<Boolean> lock(@PathVariable Long id) {
		log.info("[OZON][CONTROLLER] 锁定订单: id={}", id);
		boolean result = erpOrderService.setLocked(id, 1);
		return ApiResult.ok(result);
	}

	/**
	 * 解锁订单
	 *
	 * @param id 订单ID
	 * @return 是否成功
	 */
	@PostMapping("/unlock/{id}")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	@Operation(summary = "解锁订单")
	public ApiResult<Boolean> unlock(@PathVariable Long id) {
		log.info("[OZON][CONTROLLER] 解锁订单: id={}", id);
		boolean result = erpOrderService.setLocked(id, 0);
		return ApiResult.ok(result);
	}

	/**
	 * 批量打印面单
	 * <p>
	 * 注意：
	 * - 只支持 FBS 订单
	 * - FBO 订单会被跳过并标记为失败
	 *
	 * @param orderIds 订单ID列表
	 * @return 面单批次信息
	 */
	@PostMapping("/print-labels")
	@PreAuthorize("@per.hasPermission('order:erp-order:read')")
	@Operation(summary = "批量打印面单")
	public ApiResult<LabelBatchVO> printLabels(@RequestBody List<Long> orderIds) {
		log.info("[OZON][CONTROLLER] 批量打印面单: orderIds={}", orderIds);
		Long currentUserId = principalAttributeAccessor.getUserId();
		LabelBatchVO result = labelPrintOrchestrator.printLabels(
				PlatformEnum.Ozon.code(), orderIds, currentUserId, "批量打印");
		return ApiResult.ok(result);
	}

	/**
	 * 打印拣货单：按店铺各生成一份 Excel，供仓库照单取货。
	 * <p>
	 * 大仓与小仓订单都需要拣货单，故此处不做仓型过滤。
	 * SKU 映射缺失的行会在「备注」列标注，不会导致整单失败。
	 *
	 * @param orderIds 选中的订单ID
	 * @return 拣货单批次（每店铺一份文件 + 被排除订单的原因）
	 */
	@PostMapping("/pick-list")
	@PreAuthorize("@per.hasPermission('order:erp-order:read')")
	@Operation(summary = "打印拣货单(按店铺)")
	public ApiResult<OzonPickListBatchVO> printPickList(@RequestBody List<Long> orderIds) {
		log.info("[OZON][CONTROLLER] 打印拣货单: orderIds={}", orderIds);
		return ApiResult.ok(ozonPickListService.generate(orderIds));
	}

	/**
	 * 【准备发运】创建运单（交接单 act）。
	 * <p>
	 * 仅大仓（仓库名含「大」字）且已打印面单的 FBS 订单会被纳入。
	 * 选中订单可能跨店铺/物流方式，后端按 (店铺, 物流方式) 自动分组，一组一份运单。
	 * <p>
	 * 立即返回批次号；运单 PDF 由 Ozon 异步生成（约 1~2 分钟），
	 * 前端需轮询 {@link #getActBatch(String)} 直至全部就绪或失败。
	 *
	 * @param request 订单ID列表 + 发货日期
	 * @return 运单批次（含各运单初始状态与被排除订单的原因）
	 */
	@PostMapping("/act/create")
	@PreAuthorize("@per.hasPermission('order:erp-order:read')")
	@Operation(summary = "准备发运：创建运单(act)")
	public ApiResult<OzonActBatchVO> createActs(@RequestBody @Validated OzonActCreateDTO request) {
		log.info("[OZON][CONTROLLER] 准备发运: orderIds={}, departureDate={}",
				request.getOrderIds(), request.getDepartureDate());
		Long currentUserId = principalAttributeAccessor.getUserId();
		OzonActBatchVO result = ozonActService.createActs(
				request.getOrderIds(), request.getDepartureDate(), currentUserId);
		return ApiResult.ok(result);
	}

	/**
	 * 轮询运单批次状态。
	 * <p>
	 * 每次调用仅对未就绪的运单向 Ozon 查询一次状态；就绪则当场下载 PDF 并存入 OSS。
	 * 不阻塞、不起后台线程。
	 *
	 * @param batchNo 批次号
	 * @return 批次当前状态（READY 的运单带下载地址）
	 */
	@GetMapping("/act/batch")
	@PreAuthorize("@per.hasPermission('order:erp-order:read')")
	@Operation(summary = "轮询运单(act)批次状态")
	public ApiResult<OzonActBatchVO> getActBatch(@RequestParam String batchNo) {
		return ApiResult.ok(ozonActService.pollBatch(batchNo));
	}

	/**
	 * 同步全量订单信息，仅支持 Ozon 平台。
	 * <p>
	 * 同步所有启用店铺从 2025-01-01 开始的所有订单（FBS + FBO）
	 */
	@PostMapping("/sync-all")
	@PreAuthorize("@per.hasPermission('order:erp-order:edit')")
	@Operation(summary = "同步全量 Ozon 订单")
	public ApiResult<Void> syncAllOrders() {
		log.info("[OZON][CONTROLLER] 同步全量订单（从 2025-01-01 开始）");
		ozonOrderSyncService.syncAllShops(true);
		return ApiResult.ok();
	}

	/**
	 * 导出 Ozon 订单
	 * <p>
	 * 使用分批流式导出，避免大数据量导致内存溢出。
	 * 图片使用多线程预下载优化，提升导出性能。
	 *
	 * @param qo 查询条件
	 * @return 可迭代的数据提供者
	 */
	@PostMapping("/export")
	@PreAuthorize("@per.hasPermission('order:erp-order:read')")
	@Operation(summary = "导出 Ozon 订单")
	@AccessLoggingRule(includeQueryString = true, includeRequestBody = true)
	@ResponseExcel(name = "Ozon订单导出_#{currentDateTime()}")
	public DataFetchIterableSheetDataProvider<OzonOrderExportVO> exportOrders(@RequestBody ErpOrderQO qo) {
		log.info("[OZON][CONTROLLER] 导出订单: qo={}", qo);

		// 强制设置平台
		qo.setPlatform(PlatformEnum.Ozon.code());

		// 每批 100 条，减少内存占用
		final int batchSize = 100;

		// 创建分页数据获取函数
		BiFunction<Integer, Integer, List<OzonOrderExportVO>> pageFetcher =
				(currentPage, pageSize) -> ozonOrderQueryService.queryExportBatch(qo, currentPage, pageSize);

		// 创建分页数据获取器
		PageDataFetcher<OzonOrderExportVO> pageDataFetcher = new PageDataFetcher<>(pageFetcher, batchSize);

		// 使用图片感知数据获取器包装，实现图片多线程下载并填充到 VO
		ImageByteFetcher imageFetcher = new ImageByteFetcher(okHttpClient);
		ImageAwareDataFetcher<OzonOrderExportVO> dataFetcher = new ImageAwareDataFetcher<>(
				pageDataFetcher,
				imageFetcher
		);

		// 创建可迭代数据提供者
		return new DataFetchIterableSheetDataProvider<>(OzonOrderExportVO.class, dataFetcher);
	}
}
