package com.erp.admin.order.service.wildberries.sync;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.vo.SyncSummaryVO;
import com.erp.admin.order.service.OrderLifecycleService;
import com.erp.admin.order.service.wildberries.WbPlatformApi;
import com.erp.admin.order.service.wildberries.converter.WildberriesOrderStatusConverter;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.wildberries.credential.WbCredential;
import com.erp.admin.platform.wildberries.model.response.order.WbOrderStatus;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.util.SpringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * WbOrderStatusSyncService - Wildberries 订单状态同步服务
 * <p>
 * 职责：同步未完结订单的平台状态 → 更新 ERP 状态 → 触发库存过账副作用
 * <p>
 * 从 WbOrderSyncService 提取，专注于订单状态同步逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WbOrderStatusSyncService {

	private final ErpOrderMapper erpOrderMapper;
	private final ShopService shopService;
	private final WbPlatformApi wbPlatformApi;
	private final CredentialService credentialService;
	private final OrderLifecycleService lifecycleService;

	// ==================== 公开方法 ====================

	/**
	 * 同步所有启用店铺的未完结订单状态
	 */
	public void syncAllEnabledShopsUnfinishedOrderStatuses() {
		List<Shop> shops = shopService.listEnabledByPlatform(PlatformEnum.Wildberries.code());
		if (CollectionUtils.isEmpty(shops)) {
			log.debug("[WB][STATUS_SYNC] 没有启用的店铺");
			return;
		}

		for (Shop shop : shops) {
			try {
				TenantContext.runAs(shop.getTenantId(), () -> {
					syncShopUnfinishedOrderStatuses(shop.getId());
					return null;
				});
			} catch (Exception e) {
				log.error("[WB][STATUS_SYNC] 店铺 {} 同步未完结订单状态失败: {}", shop.getId(), e.getMessage(), e);
			}
		}
	}

	/**
	 * 同步单个店铺的未完结订单状态
	 *
	 * @param shopId 店铺ID
	 */
	public void syncShopUnfinishedOrderStatuses(Long shopId) {
		Shop shop = shopService.getById(shopId);
		if (shop == null) {
			log.warn("[WB][STATUS_SYNC] 店铺不存在 shopId={}", shopId);
			return;
		}

		// 1. 查询未完结订单（状态为 READY_TO_SHIP, SHIPPED, ARRIVED_AT_PLATFORM_WAREHOUSE）
		LambdaQueryWrapper<ErpOrder> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ErpOrder::getShopId, shopId)
				.eq(ErpOrder::getPlatform, PlatformEnum.Wildberries.code())
				.in(ErpOrder::getErpStatus, "READY_TO_SHIP", "SHIPPED", "ARRIVED_AT_PLATFORM_WAREHOUSE");

		List<ErpOrder> unfinishedOrders = erpOrderMapper.selectList(wrapper);

		if (CollectionUtils.isEmpty(unfinishedOrders)) {
			log.debug("[WB][STATUS_SYNC] 店铺 {} 没有未完结订单", shopId);
			return;
		}

		// 2. 提取订单ID列表
		List<Long> wbOrderIds = new ArrayList<>();
		for (ErpOrder order : unfinishedOrders) {
			try {
				Long wbOrderId = Long.parseLong(order.getPlatformOrderId());
				wbOrderIds.add(wbOrderId);
			} catch (NumberFormatException e) {
				log.warn("[WB][STATUS_SYNC] 无效的订单ID格式: {}", order.getPlatformOrderId());
			}
		}

		if (wbOrderIds.isEmpty()) {
			return;
		}

		log.info("[WB][STATUS_SYNC] 店铺 {} 开始同步 {} 个未完结订单状态", shopId, wbOrderIds.size());

		// 3. 批量更新订单状态
		SyncSummaryVO summary = batchUpdateOrderStatuses(shop, wbOrderIds);

		log.info("[WB][STATUS_SYNC] 店铺 {} 同步完成: 成功={}/{}", shopId, summary.getSuccess(), wbOrderIds.size());
	}

	// ==================== 内部方法 ====================

	/**
	 * 批量更新订单状态
	 */
	private SyncSummaryVO batchUpdateOrderStatuses(Shop shop, List<Long> wbOrderIds) {
		SyncSummaryVO summary = new SyncSummaryVO();
		WbCredential credential = credentialService.parseCredential(shop);

		// 1. 批量拉取订单状态
		Map<Long, WbOrderStatus> statusMap = wbPlatformApi.batchFetchOrderStatuses(credential, wbOrderIds);

		// 2. 更新订单状态
		for (Long wbOrderId : wbOrderIds) {
			try {
				ErpOrder existing = erpOrderMapper.selectOneByShopPlatformPlatformOrderId(
						shop.getId(), PlatformEnum.Wildberries.code(), String.valueOf(wbOrderId));

				if (existing == null) {
					log.warn("[WB][STATUS_SYNC] 订单不存在 shopId={} wbOrderId={}", shop.getId(), wbOrderId);
					continue;
				}

				WbOrderStatus status = statusMap.get(wbOrderId);
				if (status == null) {
					continue;
				}

				// 更新状态（通过代理调用，保证事务覆盖 update + 库存过账）
				SpringUtils.getBean(WbOrderStatusSyncService.class).doUpdateOrderStatus(existing, status);
				summary.incrSuccess();
			} catch (Exception e) {
				log.warn("[WB][STATUS_SYNC] 更新订单状态失败 wbOrderId={} msg={}", wbOrderId, e.getMessage());
			}
		}

		return summary;
	}

	/**
	 * 更新订单状态（事务方法）
	 * <p>
	 * update + 库存过账在同一事务内，保证原子性。
	 * 需通过代理调用（SpringUtils.getBean）以激活事务拦截。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void doUpdateOrderStatus(ErpOrder order, WbOrderStatus status) {
		String oldErpStatus = order.getErpStatus();

		order.setPlatformStatus(status.getWbStatus());
		if (StringUtils.hasText(status.getSupplierStatus())) {
			order.setPlatformSubstatus(status.getSupplierStatus());
		}
		String newErpStatus = WildberriesOrderStatusConverter.toErpStatus(
				status.getSupplierStatus(), status.getWbStatus());
		order.setErpStatus(newErpStatus);
		order.setSyncedAt(LocalDateTime.now());
		erpOrderMapper.updateById(order);

		// 状态变更 → 判断是否需要释放
		lifecycleService.onStatusChanged(order, oldErpStatus, newErpStatus);
	}

}
