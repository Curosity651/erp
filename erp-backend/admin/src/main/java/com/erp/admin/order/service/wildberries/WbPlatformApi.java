package com.erp.admin.order.service.wildberries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.erp.admin.order.util.LabelUtils;
import com.erp.admin.platform.PlatformApiException;
import com.erp.admin.platform.wildberries.WbSyncConstants;
import com.erp.admin.platform.wildberries.WildberriesClient;
import com.erp.admin.platform.wildberries.credential.WbCredential;
import com.erp.admin.platform.wildberries.model.request.order.WbOrderStatusRequest;
import com.erp.admin.platform.wildberries.model.request.sticker.WbGetStickersRequest;
import com.erp.admin.platform.wildberries.model.request.supply.WbCreateSupplyRequest;
import com.erp.admin.platform.wildberries.model.response.order.WbOrderStatus;
import com.erp.admin.platform.wildberries.model.response.order.WbOrderStatusResponse;
import com.erp.admin.platform.wildberries.model.response.sticker.WbStickersResponse;
import com.erp.admin.platform.wildberries.model.response.supply.WbCreateSupplyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Wildberries 平台 API 封装层
 * <p>
 * 封装所有对 WB 平台的业务级 API 操作（订单状态、面单、Supply）。
 * 只负责与平台交互，不涉及数据库操作。
 * <p>
 * 层次关系：
 * <ul>
 *   <li>{@code WildberriesClient} → 原始 HTTP 请求/响应映射</li>
 *   <li>{@code WbPlatformApi} → 业务级封装（批量分页、错误处理策略）</li>
 *   <li>{@code WbOrderSyncService} 等 → 同步编排 + DB 操作</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WbPlatformApi {

	private final WildberriesClient wbClient;

	// ==================== 订单状态 ====================

	/**
	 * 批量拉取订单状态（自动分批）
	 *
	 * @param credential WB 凭证
	 * @param orderIds   订单ID列表
	 * @return 订单ID到状态对象的映射
	 */
	public Map<Long, WbOrderStatus> batchFetchOrderStatuses(WbCredential credential, List<Long> orderIds) {
		if (orderIds == null || orderIds.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Long, WbOrderStatus> statusMap = new HashMap<>();
		List<List<Long>> batches = partition(orderIds, Math.max(1, WbSyncConstants.DEFAULT_STATUS_BATCH_SIZE));

		for (List<Long> batch : batches) {
			try {
				Map<Long, WbOrderStatus> part = fetchOrderStatusesBatch(credential, batch);
				if (part != null && !part.isEmpty()) {
					statusMap.putAll(part);
				}
			} catch (Exception e) {
				log.warn("[WB][API] 批量拉取订单状态失败: {}", e.getMessage());
			}
		}

		return statusMap;
	}

	/**
	 * 拉取单批次订单状态
	 * <p>
	 * 状态获取失败不阻断主流程，返回空 Map
	 */
	private Map<Long, WbOrderStatus> fetchOrderStatusesBatch(WbCredential credential, List<Long> platformOrderIds) {
		if (platformOrderIds == null || platformOrderIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Long, WbOrderStatus> statusMap = new HashMap<>();
		try {
			WbOrderStatusRequest request = WbOrderStatusRequest.builder()
					.orders(platformOrderIds)
					.build();
			WbOrderStatusResponse statusResp = wbClient.getOrderStatusesTyped(credential, request);
			if (statusResp != null && statusResp.getOrders() != null) {
				for (WbOrderStatus status : statusResp.getOrders()) {
					if (status.getId() != null) {
						statusMap.put(status.getId(), status);
					}
				}
			}
		} catch (PlatformApiException e) {
			log.debug("[WB][API] 批量获取状态失败: code={} size={}", e.getErrorCode(), platformOrderIds.size());
		} catch (Exception e) {
			log.warn("[WB][API] 批量获取状态异常: size={} msg={}", platformOrderIds.size(), e.getMessage());
		}
		return statusMap;
	}

	// ==================== 订单面单 ====================

	/**
	 * 获取单个订单面单
	 *
	 * @param credential      WB 凭证
	 * @param platformOrderId 平台订单ID
	 * @return Base64 编码的面单，获取失败返回 null
	 */
	public String fetchOrderLabel(WbCredential credential, Long platformOrderId) {
		if (platformOrderId == null) {
			return null;
		}

		try {
			WbGetStickersRequest request = WbGetStickersRequest.builder()
					.orders(Collections.singletonList(platformOrderId))
					.build();

			WbStickersResponse response = wbClient.getOrderStickersTyped(credential, request);

			Map<Long, String> stickerMap = LabelUtils.parseOrderStickersTyped(response);
			return stickerMap.get(platformOrderId);
		} catch (PlatformApiException e) {
			log.debug("[WB][API] 获取订单面单失败: orderId={} code={}", platformOrderId, e.getErrorCode());
			return null;
		} catch (Exception e) {
			log.warn("[WB][API] 获取订单面单异常: orderId={} msg={}", platformOrderId, e.getMessage());
			return null;
		}
	}

	// ==================== Supply 操作 ====================

	/**
	 * 创建 Supply（发货批次）
	 */
	public String createSupply(WbCredential credential, String platformOrderId) {
		String supplyName = "WB-ORD-" + platformOrderId;
		WbCreateSupplyRequest request = WbCreateSupplyRequest.builder()
				.name(supplyName)
				.build();
		WbCreateSupplyResponse response = wbClient.createSupplyTyped(credential, request);

		if (response == null || response.getId() == null) {
			throw new RuntimeException("创建 Supply 失败，返回为空");
		}
		String supplyId = response.getId();
		log.info("[WB][API] 创建 Supply 成功: name={} supplyId={}", supplyName, supplyId);
		return supplyId;
	}

	/**
	 * 添加订单到 Supply
	 */
	public void addOrderToSupply(WbCredential credential, String supplyId, String platformOrderId) {
		wbClient.addOrdersToSupply(credential, supplyId, platformOrderId);
		log.info("[WB][API] 订单已添加到 Supply: supplyId={} orderId={}", supplyId, platformOrderId);
	}

	/**
	 * 发货 Supply
	 */
	public void shipSupply(WbCredential credential, String supplyId) {
		wbClient.shipSupply(credential, supplyId);
		log.info("[WB][API] Supply 已发货: supplyId={}", supplyId);
	}

	// ==================== 工具方法 ====================

	/**
	 * 将列表分批
	 */
	private <T> List<List<T>> partition(List<T> list, int size) {
		List<List<T>> result = new ArrayList<>();
		for (int i = 0; i < list.size(); i += size) {
			result.add(list.subList(i, Math.min(i + size, list.size())));
		}
		return result;
	}

}
