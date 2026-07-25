package com.erp.admin.order.service.ozon;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.erp.admin.order.common.OrderConfirmValidator;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.enums.ErpOrderStatusEnum;
import com.erp.admin.order.service.OrderLifecycleService;
import com.erp.admin.order.service.PlatformConfirmGuard;
import com.erp.admin.order.service.common.model.ConfirmResult;
import com.erp.admin.order.service.ozon.converter.OzonOrderStatusConverter;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.ozon.credential.OzonCredential;
import com.erp.admin.platform.ozon.model.request.posting.OzonPackage;
import com.erp.admin.platform.ozon.model.response.posting.OzonPosting;
import com.erp.admin.platform.ozon.model.response.posting.OzonProduct;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Ozon 订单确认发货服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OzonOrderConfirmService {

	private final ErpOrderMapper orderMapper;
	private final OrderConfirmValidator confirmValidator;
	private final OzonPlatformApi ozonPlatformApi;
	private final ShopService shopService;
	private final ObjectMapper objectMapper;
	private final OrderLifecycleService lifecycleService;
	private final CredentialService credentialService;
	private final OzonOrderSyncService ozonOrderSyncService;
	private final PlatformConfirmGuard confirmGuard;

	/**
	 * 批量确认发货
	 */
	public ConfirmResult confirmOrders(List<Long> orderIds) {
		ConfirmResult result = new ConfirmResult();
		List<ConfirmResult.Item> items = new ArrayList<>();
		result.setItems(items);

		if (orderIds == null || orderIds.isEmpty()) {
			return result;
		}

		// 预同步：先回拉平台最新状态，避免基于陈旧本地状态发起 ship（对齐 WB/Yandex）
		try {
			ozonOrderSyncService.syncOrdersByIds(orderIds);
		} catch (Exception ex) {
			log.warn("[OZON][CONFIRM] pre-sync warn: {}", ex.getMessage());
		}

		List<ErpOrder> all = orderMapper.selectByIds(orderIds);
		if (all == null || all.isEmpty()) {
			return result;
		}

		List<ErpOrder> eligible = confirmValidator.validate(
				all, PlatformEnum.Ozon.code(),
				Collections.singletonList(ErpOrderStatusEnum.READY_TO_SHIP.name()));
		if (eligible.isEmpty()) {
			return result;
		}

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

		OzonCredential credential;
		try {
			credential = credentialService.parseCredential(shop);
		} catch (Exception ex) {
			addFailed(shopOrders, items, "店铺凭证缺失(client_id/api_key)");
			return;
		}

		for (ErpOrder order : shopOrders) {
			items.add(confirmSingleOrder(credential, order));
		}
	}

	private ConfirmResult.Item confirmSingleOrder(OzonCredential credential, ErpOrder order) {
		ConfirmResult.Item item = new ConfirmResult.Item();
		item.setOrderId(order.getId());
		if (!confirmGuard.tryClaim(order.getId())) {
			item.setSuccess(false);
			item.setMessage("该订单正在确认或已提交平台，请先同步订单状态");
			return item;
		}

		try {
			// 防御：含 GTD/产地/强制标记等额外申报要求的订单直接明确报错。
			// 本业务经营家具类目（不在俄罗斯强制标记目录内），正常不会触发；触发即说明来了特殊商品。
			String requirementsIssue = checkRequirements(order);
			if (requirementsIssue != null) {
				confirmGuard.failure(order.getId());
				item.setSuccess(false);
				item.setMessage(requirementsIssue);
				return item;
			}

			List<OzonPackage> packages = buildPackagesFromOrder(order);
			ozonPlatformApi.shipPosting(credential, order.getShipmentId(), packages);

			log.info("[OZON][CONFIRM] 订单发货请求成功 orderId={} postingNumber={}",
					order.getId(), order.getShipmentId());

			// 回拉平台状态
			try {
				Map<String, OzonPosting> postingMap = ozonPlatformApi.batchFetchPostings(
						credential, Collections.singletonList(order.getShipmentId()));

				OzonPosting posting = postingMap.get(order.getShipmentId());
				if (posting != null) {
					String oldErpStatus = order.getErpStatus();
					ErpOrderStatusEnum newErpStatus = OzonOrderStatusConverter.toErpStatus(
							posting.getStatus(), posting.getSubstatus());

					ErpOrder updateOrder = new ErpOrder();
					updateOrder.setId(order.getId());
					// null=不可映射：只更新平台状态镜像，保持 ERP 状态不变
					if (newErpStatus != null) {
						updateOrder.setErpStatus(newErpStatus.name());
					}
					updateOrder.setPlatformStatus(posting.getStatus());
					updateOrder.setPlatformSubstatus(posting.getSubstatus());
					updateOrder.setUpdateTime(LocalDateTime.now());
					orderMapper.updateById(updateOrder);

					// 触发状态变更副作用（仅在状态真实变化时）
					if (newErpStatus != null && !newErpStatus.name().equals(oldErpStatus)) {
						lifecycleService.onStatusChanged(order, oldErpStatus, newErpStatus.name());
					}

					log.info("[OZON][CONFIRM] 订单状态同步成功 orderId={} status={} -> {}",
							order.getId(), oldErpStatus, newErpStatus);
				} else {
					log.warn("[OZON][CONFIRM] 发货成功但无法获取最新状态 orderId={}", order.getId());
				}
			} catch (Exception syncEx) {
				log.error("[OZON][CONFIRM] 发货成功但状态同步失败 orderId={} error={}",
						order.getId(), syncEx.getMessage(), syncEx);
			}

			item.setSuccess(true);
			item.setMessage("OK");
			confirmGuard.success(order.getId());
		} catch (Exception ex) {
			confirmGuard.failure(order.getId());
			item.setSuccess(false);
			item.setMessage(ex.getMessage());
			log.error("[OZON][CONFIRM] 订单确认失败 orderId={} error={}",
					order.getId(), ex.getMessage(), ex);
		}

		return item;
	}

	/**
	 * 检查订单是否携带需额外申报的要求（requirements 字段中任一数组非空）
	 *
	 * @return 拦截原因；null 表示无需拦截
	 */
	private String checkRequirements(ErpOrder order) {
		try {
			if (!StringUtils.hasText(order.getRawJson())) {
				return null;
			}
			JsonNode req = objectMapper.readTree(order.getRawJson()).path("requirements");
			if (req.isMissingNode() || !req.isObject()) {
				return null;
			}
			Iterator<Map.Entry<String, JsonNode>> fields = req.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> field = fields.next();
				if (field.getValue().isArray() && field.getValue().size() > 0) {
					return "该订单包含需额外申报的商品(" + field.getKey()
							+ ")，请先在 Ozon 后台完成申报后再确认";
				}
			}
		} catch (Exception e) {
			log.warn("[OZON][CONFIRM] requirements 解析失败 orderId={} error={}",
					order.getId(), e.getMessage());
		}
		return null;
	}

	private List<OzonPackage> buildPackagesFromOrder(ErpOrder order) {
		try {
			if (!StringUtils.hasText(order.getRawJson())) {
				throw new IllegalArgumentException("订单原始数据为空，无法构建包裹");
			}
			OzonPosting ozonPosting = objectMapper.readValue(order.getRawJson(), OzonPosting.class);
			List<OzonProduct> products = ozonPosting.getProducts();
			if (products == null || products.isEmpty()) {
				throw new IllegalArgumentException("订单商品信息为空，无法构建包裹");
			}
			return ozonPlatformApi.buildPackages(products);
		} catch (Exception e) {
			log.error("[OZON][CONFIRM] 构建包裹失败 orderId={} error={}", order.getId(), e.getMessage(), e);
			throw new RuntimeException("构建发货包裹失败: " + e.getMessage(), e);
		}
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
