package com.erp.admin.order.service.wildberries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.erp.admin.order.common.OrderConfirmValidator;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.enums.ErpOrderStatusEnum;
import com.erp.admin.order.service.ErpOrderService;
import com.erp.admin.order.service.common.model.ConfirmResult;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.wildberries.credential.WbCredential;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Wildberries 订单确认发货服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WbOrderConfirmService {

	private final ErpOrderMapper orderMapper;
	private final OrderConfirmValidator confirmValidator;
	private final ShopService shopService;
	private final CredentialService credentialService;
	private final WbPlatformApi wbPlatformApi;
	private final WbOrderSyncService wbOrderSyncService;
	private final ErpOrderService erpOrderService;

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

		// 预同步
		try {
			wbOrderSyncService.syncOrdersByIds(orderIds);
		} catch (Exception ex) {
			log.warn("[WB][CONFIRM] pre-sync warn: {}", ex.getMessage());
		}

		// 查询订单
		List<ErpOrder> all = orderMapper.selectByIds(orderIds);
		if (CollectionUtils.isEmpty(all)) {
			return result;
		}

		// 验证
		List<ErpOrder> eligible = confirmValidator.validate(
				all, PlatformEnum.Wildberries.code(),
				Collections.singletonList(ErpOrderStatusEnum.READY_TO_SHIP.name()));
		if (eligible.isEmpty()) {
			return result;
		}

		// 按店铺分组
		Map<Long, List<ErpOrder>> byShop = new HashMap<>();
		for (ErpOrder o : eligible) {
			if (o == null || o.getShopId() == null) continue;
			byShop.computeIfAbsent(o.getShopId(), k -> new ArrayList<>()).add(o);
		}

		// 逐店铺确认
		for (Map.Entry<Long, List<ErpOrder>> entry : byShop.entrySet()) {
			Long shopId = entry.getKey();
			List<ErpOrder> shopOrders = entry.getValue();
			if (CollectionUtils.isEmpty(shopOrders)) continue;

			WbCredential credential;
			try {
				Shop shop = shopService.getById(shopId);
				credential = credentialService.parseCredential(shop);
			} catch (Exception ex) {
				for (ErpOrder order : shopOrders) {
					ConfirmResult.Item it = new ConfirmResult.Item();
					it.setOrderId(order.getId());
					it.setSuccess(false);
					it.setMessage("店铺凭证缺失(api_key)");
					items.add(it);
				}
				continue;
			}

			for (ErpOrder order : shopOrders) {
				ConfirmResult.Item it = new ConfirmResult.Item();
				it.setOrderId(order.getId());
				try {
					String supplyId = order.getShipmentId();

					if (!StringUtils.hasText(supplyId)) {
						supplyId = wbPlatformApi.createSupply(credential, order.getPlatformOrderId());
						erpOrderService.updateShipmentId(order.getId(), supplyId);
						wbPlatformApi.addOrderToSupply(credential, supplyId, order.getPlatformOrderId());
					}

					wbPlatformApi.shipSupply(credential, supplyId);

					it.setSuccess(true);
					it.setSupplyId(supplyId);
					it.setMessage("OK");
				} catch (Exception ex) {
					it.setSuccess(false);
					it.setMessage(ex.getMessage());
				}
				items.add(it);
			}
		}

		// 后同步
		try {
			wbOrderSyncService.syncOrdersByIds(orderIds);
		} catch (Exception ex) {
			log.warn("[WB][CONFIRM] post-sync warn: {}", ex.getMessage());
		}

		return result;
	}

}
