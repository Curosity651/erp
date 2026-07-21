package com.erp.admin.order.service.wildberries;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.dto.wildberries.WbOrderSyncDTO;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.vo.SyncSummaryVO;
import com.erp.admin.order.service.OrderLifecycleService;
import com.erp.admin.order.service.wildberries.converter.WbOrderConverter;
import com.erp.admin.platform.PlatformApiException;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.sync.service.SyncCursorService;
import com.erp.admin.sync.model.enums.SyncTaskTypeEnum;
import com.erp.admin.platform.wildberries.credential.WbCredential;
import com.erp.admin.platform.wildberries.WbSyncConstants;
import com.erp.admin.platform.wildberries.WildberriesClient;
import com.erp.admin.platform.wildberries.config.WildberriesSyncProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.admin.platform.wildberries.model.common.WbPaginationQuery;
import com.erp.admin.platform.wildberries.model.response.order.WbOrder;
import com.erp.admin.platform.wildberries.model.response.order.WbOrderListResponse;
import com.erp.admin.platform.wildberries.model.response.order.WbOrderStatus;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * WbOrderSyncService - Wildberries 订单同步服务
 * <p>
 * 职责：
 * - 订单列表同步（全量/增量）
 * - 订单状态同步
 * - 按订单ID同步
 * <p>
 * 从 WbSyncService 提取，专注于订单同步逻辑
 *
 * @author system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WbOrderSyncService {

	private final WildberriesClient wbClient;
	private final ErpOrderMapper erpOrderMapper;
	private final ObjectMapper objectMapper;
	private final ShopService shopService;
	private final WildberriesSyncProperties wildberriesSyncProperties;
	private final WbOrderUpsertService wbOrderUpsertService;
	private final WbPlatformApi wbPlatformApi;
	private final OrderLifecycleService lifecycleService;
	private final CredentialService credentialService;
	private final SyncCursorService syncCursorService;

	/**
	 * 默认起始日期：2025-01-01
	 */
	private static final LocalDateTime DEFAULT_START_DATE = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

	// ==================== 订单列表同步 ====================

	/**
	 * 同步所有启用的店铺的订单
	 *
	 * @param fetchAll 是否拉取所有订单（首次同步）
	 */
	public void syncAllEnabledShopOrders(boolean fetchAll) {
		List<Shop> shops = shopService.listEnabledByPlatform(PlatformEnum.Wildberries.code());
		if (CollectionUtils.isEmpty(shops)) {
			log.debug("[WB][SYNC] 没有启用的店铺");
			return;
		}

		for (Shop shop : shops) {
			try {
				TenantContext.runAs(shop.getTenantId(), () -> {
					syncShopOrder(shop, fetchAll);
					return null;
				});
			} catch (Exception e) {
				log.error("[WB][SYNC] 店铺 {} 同步失败: {}", shop.getId(), e.getMessage(), e);
			}
		}
	}

	/**
	 * 同步单个店铺的订单
	 *
	 * @param shop     店铺对象
	 * @param fetchAll 是否拉取所有订单
	 */
	public void syncShopOrder(Shop shop, boolean fetchAll) {
		if (!wildberriesSyncProperties.isEnabled()) {
			log.debug("[WB][SYNC] 同步已禁用");
			return;
		}

		// 1. 计算同步窗口
		LocalDateTime windowStart = resolveWindowStart(shop.getId(), fetchAll);
		LocalDateTime windowEnd = LocalDateTime.now(ZoneOffset.UTC);

		// 2. 兜底：避免 dateFrom > dateTo
		if (windowStart.isAfter(windowEnd)) {
			windowStart = windowEnd.minusDays(1);
		}

		log.info("[WB][SYNC] 店铺 {} 同步窗口: {} ~ {}", shop.getId(), windowStart, windowEnd);

		// 3. 执行同步
		syncByWindow(shop, windowStart, windowEnd);
	}

	/**
	 * 按时间窗口同步订单（支持大时间范围切分）
	 *
	 * @param shop        店铺对象
	 * @param windowStart 窗口起始时间
	 * @param windowEnd   窗口结束时间
	 */
	private void syncByWindow(Shop shop, LocalDateTime windowStart, LocalDateTime windowEnd) {
		WbCredential credential = credentialService.parseCredential(shop);

		int totalProcessed = 0;
		int totalSuccess = 0;

		// 按窗口步长分段拉取（WB API 限制查询时间范围在一个月内）
		LocalDateTime cursor = windowStart;
		while (!cursor.isAfter(windowEnd)) {
			LocalDateTime segmentEnd = cursor.plusDays(WbSyncConstants.DEFAULT_WINDOW_STEP_DAYS);
			if (segmentEnd.isAfter(windowEnd)) {
				segmentEnd = windowEnd;
			}

			long dateFrom = cursor.toEpochSecond(ZoneOffset.UTC);
			long dateTo = segmentEnd.toEpochSecond(ZoneOffset.UTC);

			log.debug("[WB][SYNC] 店铺 {} 同步片段: {} ~ {}", shop.getId(), cursor, segmentEnd);

			// 分页拉取当前时间段的订单
			boolean segmentFailed = false;
			long next = 0L;
			int pageIteration = 0;

			while (pageIteration < WbSyncConstants.MAX_PAGE_ITERATIONS) {
				pageIteration++;

				try {
					// 构建分页查询参数
					WbPaginationQuery query = WbPaginationQuery.builder()
							.limit(WbSyncConstants.DEFAULT_ORDER_PAGE_SIZE)
							.next(next)
							.dateFrom(dateFrom)
							.dateTo(dateTo)
							.build();

					// 直接调用 Client 获取订单
					WbOrderListResponse response = wbClient.getOrdersTyped(credential, query);

					if (response == null || CollectionUtils.isEmpty(response.getOrders())) {
						break;
					}

					// 处理订单批次
					SyncResult result = processOrderBatch(shop, credential, response.getOrders(), null);

					totalProcessed += result.processed;
					totalSuccess += result.success;
					if (result.success < result.processed) { segmentFailed = true; }

					// 检查下一页
					if (response.getNext() == null) {
						break;
					}

					long newNext = response.getNext();
					if (newNext == next) {
						break; // 避免死循环
					}
					next = newNext;

				} catch (PlatformApiException e) {
					segmentFailed = true;
					// 认证错误：记录并跳出，不继续拉取
					if ("UNAUTHORIZED".equals(e.getErrorCode())) {
						log.error("[WB][SYNC] 店铺 {} 认证失败，停止同步", shop.getId());
						break;
					}
					// 其他平台错误（限流、网络、上游错误）：HttpExecutor 已记录详细日志，这里跳出当前窗口
					log.warn("[WB][SYNC] 店铺 {} 拉取订单失败: code={}", shop.getId(), e.getErrorCode());
					break;
				} catch (Exception e) {
					segmentFailed = true;
					log.error("[WB][SYNC] 店铺 {} 拉取订单异常: {}", shop.getId(), e.getMessage(), e);
					break;
				}
			}

			if (segmentFailed) {
				log.warn("[WB][SYNC] 店铺 {} 片段 {}~{} 存在失败，保留游标不推进，下轮重试（若持续失败请人工排查坏单）",
							shop.getId(), cursor, segmentEnd);
				break;
			}

			// 移动到下一个窗口
			cursor = segmentEnd.plusSeconds(1);

			// 阶段性同步完成，推进游标
			syncCursorService.advanceCursor(
					shop.getId(), PlatformEnum.Wildberries.code(), SyncTaskTypeEnum.ORDER_INCREMENTAL, segmentEnd);
		}

		log.info("[WB][SYNC] 店铺 {} 同步完成: 处理={} 成功={}", shop.getId(), totalProcessed, totalSuccess);
	}


	/**
	 * 处理订单列表（转换、补充状态、持久化）
	 *
	 * @param shop           店铺对象
	 * @param credential     凭证
	 * @param orders         订单列表
	 * @param targetOrderIds 目标订单ID集合（可选，用于判断是否找到目标订单）
	 * @return 处理结果统计
	 */
	private SyncResult processOrderBatch(
			Shop shop,
			WbCredential credential,
			List<WbOrder> orders,
			Set<Long> targetOrderIds) {

		SyncResult result = new SyncResult();

		// 1. 收集订单ID
		List<Long> orderIds = new ArrayList<>();
		for (WbOrder order : orders) {
			if (order.getId() != null) {
				orderIds.add(order.getId());

				// 检查是否找到目标订单
				if (targetOrderIds != null && targetOrderIds.contains(order.getId())) {
					result.foundTargetIds.add(order.getId());
				}
			}
		}

		// 2. 批量拉取订单状态
		Map<Long, WbOrderStatus> statusMap = orderIds.isEmpty() ? new HashMap<>()
				: wbPlatformApi.batchFetchOrderStatuses(credential, orderIds);

		// 3. 处理每个订单
		for (WbOrder order : orders) {
			if (order.getId() == null) {
				continue;
			}

			try {
				// 转换为 DTO
				WbOrderSyncDTO dto = WbOrderConverter.toDTO(order, objectMapper);
				if (dto == null) {
					continue;
				}

				// 用状态接口的结果覆盖/补齐
				WbOrderStatus status = statusMap.get(order.getId());
				if (status != null) {
					if (StringUtils.hasText(status.getWbStatus())) {
						dto.setWbStatus(status.getWbStatus());
					}
					if (StringUtils.hasText(status.getSupplierStatus())) {
						dto.setSupplierStatus(status.getSupplierStatus());
					}
				}

				// Upsert 订单
				wbOrderUpsertService.upsertOrder(shop.getId(), dto);

				result.processed++;
				result.success++;
			} catch (Exception e) {
				log.error("[WB][SYNC] 处理订单失败 orderId={}", order.getId(), e);
				result.processed++;
			}
		}

		return result;
	}

	// ==================== 按订单ID同步 ====================

	/**
	 * 根据ERP订单ID列表同步订单完整信息
	 * <p>
	 * 此方法会：
	 * 1. 批量查询订单
	 * 2. 按店铺分组
	 * 3. 对每个店铺调用 syncOrdersByIdsForShop 进行同步
	 *
	 * @param orderIds ERP订单ID列表
	 * @return 同步摘要
	 */
	public SyncSummaryVO syncOrdersByIds(List<Long> orderIds) {
		SyncSummaryVO summary = new SyncSummaryVO();

		if (orderIds == null || orderIds.isEmpty()) {
			return summary;
		}

		// 1. 批量查询订单
		List<ErpOrder> orders = erpOrderMapper.selectBatchIds(orderIds);
		if (orders == null || orders.isEmpty()) {
			log.warn("[WB][SYNC_BY_ID] 未查询到任何订单");
			return summary;
		}

		summary.setTotal(orders.size());

		// 2. 按店铺分组（只处理 Wildberries 平台）
		Map<Long, List<ErpOrder>> shopOrderMap = new HashMap<>();
		for (ErpOrder order : orders) {
			if (order == null) {
				continue;
			}

			// 只处理 Wildberries 平台订单
			if (!PlatformEnum.Wildberries.code().equalsIgnoreCase(order.getPlatform())) {
				log.debug("[WB][SYNC_BY_ID] 跳过非WB订单 orderId={} platform={}", order.getId(), order.getPlatform());
				summary.incrSkipped();
				continue;
			}

			shopOrderMap.computeIfAbsent(order.getShopId(), k -> new ArrayList<>()).add(order);
		}

		// 3. 按店铺同步
		for (Map.Entry<Long, List<ErpOrder>> entry : shopOrderMap.entrySet()) {
			Long shopId = entry.getKey();
			List<ErpOrder> shopOrders = entry.getValue();

			if (shopOrders == null || shopOrders.isEmpty()) {
				continue;
			}

			try {
				Shop shop = shopService.getById(shopId);
				if (shop == null) {
					log.warn("[WB][SYNC_BY_ID] 店铺不存在 shopId={}", shopId);
					for (int i = 0; i < shopOrders.size(); i++) {
						summary.incrSkipped();
					}
					continue;
				}

				// 调用店铺级别的同步方法
				SyncSummaryVO shopSummary = syncOrdersByIdsForShop(shop, shopOrders);
				summary.merge(shopSummary);

			} catch (Exception e) {
				log.error("[WB][SYNC_BY_ID] 同步店铺订单失败 shopId={} msg={}", shopId, e.getMessage(), e);
				for (int i = 0; i < shopOrders.size(); i++) {
					summary.incrFailed();
				}
			}
		}

		return summary;
	}

	/**
	 * 同步单个店铺的订单（内部方法）
	 *
	 * @param shop   店铺对象
	 * @param orders 订单列表
	 * @return 同步摘要
	 */
	private SyncSummaryVO syncOrdersByIdsForShop(Shop shop, List<ErpOrder> orders) {
		SyncSummaryVO summary = new SyncSummaryVO();
		WbCredential credential = credentialService.parseCredential(shop);

		// 收集需要查找的平台订单ID及其订单信息
		Map<Long, ErpOrder> platformOrderMap = new HashMap<>();

		for (ErpOrder order : orders) {
			try {
				if (!order.getShopId().equals(shop.getId())) {
					log.warn("[WB][SYNC_BY_ID] 订单不属于当前店铺 orderId={} shopId={}", order.getId(), shop.getId());
					summary.incrSkipped();
					continue;
				}

				Long platformOrderId = Long.parseLong(order.getPlatformOrderId());
				platformOrderMap.put(platformOrderId, order);

			} catch (Exception e) {
				log.warn("[WB][SYNC_BY_ID] 解析订单失败 orderId={} msg={}", order.getId(), e.getMessage());
				summary.incrSkipped();
			}
		}

		if (platformOrderMap.isEmpty()) {
			return summary;
		}

		log.info("[WB][SYNC_BY_ID] 店铺 {} 开始同步 {} 个订单", shop.getId(), platformOrderMap.size());

		// 按订单创建时间分组（相近时间的订单可以在一个窗口内查询）
		Map<TimeWindow, List<Long>> windowGroups = groupOrdersByTimeWindow(platformOrderMap);

		Set<Long> allTargetIds = platformOrderMap.keySet();
		Set<Long> foundIds = new HashSet<>();

		// 对每个时间窗口进行查询
		for (Map.Entry<TimeWindow, List<Long>> entry : windowGroups.entrySet()) {
			TimeWindow window = entry.getKey();
			List<Long> targetIds = entry.getValue();

			try {
				Set<Long> found = syncOrdersInTimeWindow(
						shop, credential, window, new HashSet<>(targetIds), summary);
				foundIds.addAll(found);
			} catch (Exception e) {
				log.error("[WB][SYNC_BY_ID] 时间窗口同步失败 window={} msg={}", window, e.getMessage());
			}
		}

		// 统计未找到的订单
		Set<Long> notFound = new HashSet<>(allTargetIds);
		notFound.removeAll(foundIds);

		if (!notFound.isEmpty()) {
			log.warn("[WB][SYNC_BY_ID] {} 个订单未在平台找到: {}", notFound.size(), notFound);
			summary.addFailed(notFound.size());
		}

		log.info("[WB][SYNC_BY_ID] 店铺 {} 同步完成: 成功={} 未找到={}", shop.getId(), foundIds.size(), notFound.size());

		return summary;
	}


	/**
	 * 在指定时间窗口内同步订单
	 *
	 * @param shop           店铺对象
	 * @param credential     凭证
	 * @param window         时间窗口
	 * @param targetOrderIds 目标订单ID集合
	 * @param summary        同步摘要
	 * @return 找到的目标订单ID集合
	 */
	private Set<Long> syncOrdersInTimeWindow(
			Shop shop,
			WbCredential credential,
			TimeWindow window,
			Set<Long> targetOrderIds,
			SyncSummaryVO summary) {

		Set<Long> foundIds = new HashSet<>();

		long dateFrom = window.start.toEpochSecond(ZoneOffset.UTC);
		long dateTo = window.end.toEpochSecond(ZoneOffset.UTC);

		log.debug("[WB][SYNC_BY_ID] 查询时间窗口: {} ~ {}", window.start, window.end);

		// 分页查询
		long next = 0L;
		int maxPages = 10; // 限制最多10页，避免时间窗口过大

		for (int page = 0; page < maxPages; page++) {
			try {
				WbPaginationQuery query = WbPaginationQuery.builder()
						.limit(WbSyncConstants.DEFAULT_ORDER_PAGE_SIZE)
						.next(next)
						.dateFrom(dateFrom)
						.dateTo(dateTo)
						.build();

				// 直接调用 Client 获取订单
				WbOrderListResponse response = wbClient.getOrdersTyped(credential, query);

				if (response == null || CollectionUtils.isEmpty(response.getOrders())) {
					break;
				}

				// 处理本页订单
				SyncResult result = processOrderBatch(shop, credential, response.getOrders(), targetOrderIds);

				// 累加成功数量
				for (int i = 0; i < result.success; i++) {
					summary.incrSuccess();
				}
				foundIds.addAll(result.foundTargetIds);

				// 如果已经找到所有目标订单，可以提前退出
				if (foundIds.containsAll(targetOrderIds)) {
					log.debug("[WB][SYNC_BY_ID] 已找到所有目标订单，提前退出");
					break;
				}

				// 检查下一页
				if (response.getNext() == null || response.getNext() == next) {
					break;
				}
				next = response.getNext();

			} catch (PlatformApiException e) {
				// 认证错误：记录并跳出
				if ("UNAUTHORIZED".equals(e.getErrorCode())) {
					log.error("[WB][SYNC_BY_ID] 认证失败，停止查询");
					break;
				}
				// 其他平台错误：HttpExecutor 已记录详细日志
				log.warn("[WB][SYNC_BY_ID] 分页查询失败: code={}", e.getErrorCode());
				break;
			} catch (Exception e) {
				log.error("[WB][SYNC_BY_ID] 分页查询异常: {}", e.getMessage());
				break;
			}
		}

		return foundIds;
	}

	/**
	 * 按时间窗口分组订单（前后5秒）
	 *
	 * @param orderMap 平台订单ID到订单实体的映射
	 * @return 时间窗口到订单ID列表的映射
	 */
	private Map<TimeWindow, List<Long>> groupOrdersByTimeWindow(Map<Long, ErpOrder> orderMap) {
		Map<TimeWindow, List<Long>> groups = new HashMap<>();

		for (Map.Entry<Long, ErpOrder> entry : orderMap.entrySet()) {
			Long platformOrderId = entry.getKey();
			ErpOrder order = entry.getValue();

			LocalDateTime baseTime = order.getPlatformCreatedAt() != null
					? order.getPlatformCreatedAt()
					: order.getCreateTime();

			if (baseTime == null) {
				baseTime = LocalDateTime.now(ZoneOffset.UTC);
			}

			// 前后5秒窗口
			TimeWindow window = new TimeWindow(
					baseTime.minusSeconds(5),
					baseTime.plusSeconds(5)
			);

			groups.computeIfAbsent(window, k -> new ArrayList<>()).add(platformOrderId);
		}

		return groups;
	}

	// ==================== 辅助方法 ====================

	/**
	 * 计算同步窗口起始时间
	 *
	 * @param shopId   店铺ID
	 * @param fetchAll 是否拉取所有订单
	 * @return 窗口起始时间
	 */
	private LocalDateTime resolveWindowStart(Long shopId, boolean fetchAll) {
		if (fetchAll) {
			return DEFAULT_START_DATE;
		}

		try {
			LocalDateTime cursor = syncCursorService.getCursor(
					shopId, PlatformEnum.Wildberries.code(), SyncTaskTypeEnum.ORDER_INCREMENTAL);

			if (cursor == null) {
				log.info("[WB][SYNC] 店铺 {} 首次同步，从 {} 开始", shopId, DEFAULT_START_DATE);
				return DEFAULT_START_DATE;
			}

			LocalDateTime startTime = cursor.minusMinutes(wildberriesSyncProperties.getLookbackMinutes());

			log.info("[WB][SYNC] 店铺 {} 增量同步，游标={}, 回溯 {}min, 起点={}",
					shopId, cursor, wildberriesSyncProperties.getLookbackMinutes(), startTime);
			return startTime;

		} catch (Exception e) {
			log.warn("[WB][SYNC] 获取游标失败，使用默认回溯: {}", e.getMessage());
			return LocalDateTime.now(ZoneOffset.UTC).minusDays(WbSyncConstants.DEFAULT_ERROR_FALLBACK_DAYS);
		}
	}

	// ==================== 内部类 ====================

	/**
	 * 同步结果统计（内部使用）
	 */
	private static class SyncResult {
		int processed = 0;
		int success = 0;
		Set<Long> foundTargetIds = new HashSet<>();
	}

	/**
	 * 时间窗口（用于分组订单）
	 */
	private static class TimeWindow {
		final LocalDateTime start;
		final LocalDateTime end;

		TimeWindow(LocalDateTime start, LocalDateTime end) {
			this.start = start;
			this.end = end;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TimeWindow that = (TimeWindow) o;
			return start.equals(that.start) && end.equals(that.end);
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hash(start, end);
		}

		@Override
		public String toString() {
			return start + " ~ " + end;
		}
	}

}
