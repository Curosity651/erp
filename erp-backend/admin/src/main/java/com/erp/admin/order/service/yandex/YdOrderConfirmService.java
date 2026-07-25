package com.erp.admin.order.service.yandex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.order.common.OrderConfirmValidator;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.enums.ErpOrderStatusEnum;
import com.erp.admin.order.service.common.model.ConfirmResult;
import com.erp.admin.order.service.PlatformConfirmGuard;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.yandex.YandexClient;
import com.erp.admin.platform.yandex.credential.YandexCredential;
import com.erp.admin.platform.yandex.enums.YandexOrderStatusEnum;
import com.erp.admin.platform.yandex.enums.YandexOrderSubstatusEnum;
import com.erp.admin.platform.yandex.model.request.order.YandexSetBoxLayoutRequest;
import com.erp.admin.platform.yandex.model.request.order.YandexUpdateOrderStatusRequest;
import com.erp.admin.platform.yandex.model.response.order.YandexGetOrdersResponse.YandexOrder;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Yandex 订单确认发货服务
 * <p>
 * 两步操作：装箱(setOrderBoxLayout) → 状态变更(PROCESSING/READY_TO_SHIP)。
 * <p>
 * 不调用发货批次确认(confirmShipment)。实测依据（2026-07-02，真实店铺，样本为 2026-06-25~07-01
 * 的两个批次及其订单，shipmentType=IMPORT 自送分拣中心模式）：批次从未确认、从未签署
 * （signed=false / factCount=0 / availableActions 仍含 CONFIRM），到达计划窗口终点即自动 FINISHED，
 * 订单靠分拣中心逐件扫码正常推进至 DELIVERED —— 批次确认对该模式非流程必需。
 * 注意边界：若启用快递揽收(WITHDRAW)模式仓库需重新评估；未签署交接单在丢件纠纷时缺少交接凭据。
 * 实物交付的跟踪由海外仓平台租户的出库作业（下架→打包→签出）负责，ERP 侧不做重复监控。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YdOrderConfirmService {

	private final ErpOrderMapper orderMapper;
	private final OrderConfirmValidator confirmValidator;
	private final YandexClient yandexClient;
	private final ShopService shopService;
	private final YdPlatformApi ydPlatformApi;
	private final YdOrderUpsertService ydOrderUpsertService;
	private final YdOrderSyncService ydOrderSyncService;
	private final ObjectMapper objectMapper;
	private final CredentialService credentialService;
	private final PlatformConfirmGuard confirmGuard;

	/**
	 * 批量确认发货
	 */
	public ConfirmResult confirmOrders(List<Long> orderIds) {
		ConfirmResult result = new ConfirmResult();
		List<ConfirmResult.Item> items = new ArrayList<>();
		result.setItems(items);

		if (CollectionUtils.isEmpty(orderIds)) {
			return result;
		}

		// 预同步：先回拉平台最新状态，避免基于陈旧本地状态发起装箱/状态变更（对齐 WB）
		try {
			ydOrderSyncService.syncOrdersByIds(orderIds);
		} catch (Exception ex) {
			log.warn("[YANDEX][CONFIRM] pre-sync warn: {}", ex.getMessage());
		}

		// 1. 查询订单
		List<ErpOrder> all = orderMapper.selectByIds(orderIds);
		if (CollectionUtils.isEmpty(all)) {
			return result;
		}

		// 2. 校验
		List<ErpOrder> eligible = confirmValidator.validate(
				all, PlatformEnum.Yandex.code(),
				Collections.singletonList(ErpOrderStatusEnum.READY_TO_SHIP.name()));
		if (eligible.isEmpty()) {
			return result;
		}

		// 3. 按 shopId 分组
		Map<Long, List<ErpOrder>> groupByShop = eligible.stream()
				.collect(Collectors.groupingBy(ErpOrder::getShopId));

		for (Map.Entry<Long, List<ErpOrder>> entry : groupByShop.entrySet()) {
			processShopOrders(entry.getKey(), entry.getValue(), items);
		}

		return result;
	}

	private void processShopOrders(Long shopId, List<ErpOrder> shopOrders, List<ConfirmResult.Item> items) {
		Shop shop = shopService.getById(shopId);
		if (shop == null) {
			addFailed(shopOrders, items, "店铺不存在");
			return;
		}

		YandexCredential credential;
		try {
			credential = credentialService.parseCredential(shop);
		} catch (Exception ex) {
			addFailed(shopOrders, items, "店铺凭证缺失(api_key/business_id/campaign_id)");
			return;
		}

		// 逐单确认
		for (ErpOrder order : shopOrders) {
			items.add(confirmSingleOrder(credential, order));
		}

		// 回拉最新状态
		try {
			Set<Long> ydOrderIds = shopOrders.stream()
					.map(ErpOrder::getPlatformOrderId)
					.filter(StringUtils::hasText)
					.map(pid -> {
						try { return Long.parseLong(pid); }
						catch (NumberFormatException e) { return null; }
					})
					.filter(java.util.Objects::nonNull)
					.collect(Collectors.toSet());
			List<YandexOrder> latestOrders = ydPlatformApi.fetchOrdersByIds(credential, ydOrderIds);
			for (YandexOrder ydOrder : latestOrders) {
				try {
					ydOrderUpsertService.upsertOrder(shopId, ydOrder);
				} catch (Exception ex) {
					log.warn("[YANDEX][CONFIRM] 回拉状态更新失败 orderId={} error={}",
							ydOrder.getOrderId(), ex.getMessage());
				}
			}
		} catch (Exception ex) {
			log.warn("[YANDEX][CONFIRM] 回拉订单状态失败 shopId={} error={}",
					shopId, ex.getMessage());
		}
	}

	private ConfirmResult.Item confirmSingleOrder(YandexCredential credential, ErpOrder order) {
		ConfirmResult.Item item = new ConfirmResult.Item();
		item.setOrderId(order.getId());
		if (!confirmGuard.tryClaim(order.getId())) {
			item.setSuccess(false);
			item.setMessage("该订单正在确认或已提交平台，请先同步订单状态");
			return item;
		}

		try {
			if (!StringUtils.hasText(order.getPlatformOrderId())) {
				confirmGuard.failure(order.getId());
				item.setSuccess(false);
				item.setMessage("缺少平台订单号");
				return item;
			}
			long ydOrderId;
			try {
				ydOrderId = Long.parseLong(order.getPlatformOrderId());
			} catch (NumberFormatException e) {
				confirmGuard.failure(order.getId());
				item.setSuccess(false);
				item.setMessage("平台订单号格式异常: " + order.getPlatformOrderId());
				return item;
			}

			// Step 1 & 2: 装箱 + 状态变更（如果未完成）
			if (!YandexOrderSubstatusEnum.READY_TO_SHIP.name().equals(order.getPlatformSubstatus())) {
				// 装箱
				YandexSetBoxLayoutRequest boxRequest = buildBoxRequest(order);
				yandexClient.setOrderBoxLayout(credential, ydOrderId, boxRequest);
				log.info("[YANDEX][CONFIRM] 装箱成功 orderId={} ydOrderId={}", order.getId(), ydOrderId);

				// 状态变更 → PROCESSING/READY_TO_SHIP
				YandexUpdateOrderStatusRequest statusRequest = YandexUpdateOrderStatusRequest.builder()
						.order(YandexUpdateOrderStatusRequest.OrderStatusChange.builder()
								.status(YandexOrderStatusEnum.PROCESSING.name())
								.substatus(YandexOrderSubstatusEnum.READY_TO_SHIP.name())
								.build())
						.build();
				yandexClient.updateOrderStatus(credential, ydOrderId, statusRequest);
				log.info("[YANDEX][CONFIRM] 状态变更成功 orderId={} → PROCESSING/READY_TO_SHIP", order.getId());
			} else {
				log.info("[YANDEX][CONFIRM] 跳过装箱和状态变更（已完成） orderId={}", order.getId());
			}

			item.setSuccess(true);
			item.setMessage("OK");
			confirmGuard.success(order.getId());
		} catch (Exception ex) {
			confirmGuard.failure(order.getId());
			item.setSuccess(false);
			item.setMessage(ex.getMessage());
			log.error("[YANDEX][CONFIRM] 确认失败 orderId={} error={}",
					order.getId(), ex.getMessage(), ex);
		}

		return item;
	}

	/**
	 * 从 rawJson 构建装箱请求（所有商品放入一个箱子）
	 */
	private YandexSetBoxLayoutRequest buildBoxRequest(ErpOrder order) {
		List<YandexSetBoxLayoutRequest.BoxLayoutItem> boxItems = new ArrayList<>();

		try {
			JsonNode root = objectMapper.readTree(order.getRawJson());
			JsonNode itemsNode = root.path("items");
			if (itemsNode.isArray()) {
				for (JsonNode itemNode : itemsNode) {
					Long itemId = itemNode.path("id").asLong();
					int count = itemNode.path("count").asInt(1);
					boxItems.add(YandexSetBoxLayoutRequest.BoxLayoutItem.builder()
							.id(itemId)
							.fullCount(count)
							.build());
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("解析订单商品信息失败: " + e.getMessage(), e);
		}

		if (boxItems.isEmpty()) {
			throw new RuntimeException("订单商品信息为空，无法装箱");
		}

		return YandexSetBoxLayoutRequest.builder()
				.boxes(Collections.singletonList(
						YandexSetBoxLayoutRequest.BoxLayout.builder()
								.items(boxItems)
								.build()))
				.build();
	}

	private void addFailed(List<ErpOrder> orders, List<ConfirmResult.Item> items, String message) {
		for (ErpOrder order : orders) {
			ConfirmResult.Item item = new ConfirmResult.Item();
			item.setOrderId(order.getId());
			item.setSuccess(false);
			item.setMessage(message);
			items.add(item);
		}
	}
}
