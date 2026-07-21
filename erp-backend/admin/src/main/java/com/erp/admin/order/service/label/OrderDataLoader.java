package com.erp.admin.order.service.label;

import com.erp.admin.order.mapper.ErpOrderItemMapper;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.service.SkuMappingService;
import com.erp.admin.product.service.SkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单数据加载组件
 * <p>
 * 职责：
 * - 批量加载订单
 * - 批量查询 SKU 映射
 * <p>
 * 使用场景：WB 和 Ozon 面单打印服务
 *
 * @author system
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderDataLoader {

	private final ErpOrderMapper erpOrderMapper;
	private final ErpOrderItemMapper orderItemMapper;
	private final SkuService skuService;
	private final SkuMappingService skuMappingService;

	/**
	 * 批量加载订单
	 *
	 * @param orderIds 订单ID列表
	 * @param platform 平台代码
	 * @return 订单列表
	 */
	public List<ErpOrder> loadOrders(List<Long> orderIds, String platform) {
		if (orderIds == null || orderIds.isEmpty()) {
			return Collections.emptyList();
		}

		List<ErpOrder> orders = erpOrderMapper.selectByIds(orderIds);
		if (orders == null) {
			return Collections.emptyList();
		}

		List<ErpOrder> filtered = orders.stream()
				.filter(o -> o != null && platform.equalsIgnoreCase(o.getPlatform()))
				.collect(Collectors.toList());

		// 批量加载 items（避免 N+1）
		if (!filtered.isEmpty()) {
			List<Long> ids = filtered.stream().map(ErpOrder::getId).collect(Collectors.toList());
			List<ErpOrderItem> allItems = orderItemMapper.selectByOrderIds(ids);
			Map<Long, List<ErpOrderItem>> itemMap = allItems.stream()
					.collect(Collectors.groupingBy(ErpOrderItem::getOrderId));
			filtered.forEach(o -> o.setItems(
					itemMap.getOrDefault(o.getId(), Collections.emptyList())));
		}

		return filtered;
	}

	/**
	 * 批量查询 SKU 实体（通过 platformItemId → sku_mapping → Sku）
	 *
	 * @param orders 订单列表（items 已加载）
	 * @return skuCode -> Sku 映射
	 */
	public Map<String, Sku> loadSkuMap(List<ErpOrder> orders) {
		if (orders == null || orders.isEmpty()) {
			return Collections.emptyMap();
		}

		Set<String> platformItemIds = orders.stream()
				.filter(o -> o.getItems() != null)
				.flatMap(o -> o.getItems().stream())
				.map(ErpOrderItem::getPlatformItemId)
				.filter(StringUtils::hasText)
				.collect(Collectors.toSet());

		if (platformItemIds.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, String> skuCodeMap = skuMappingService
				.getSkuCodeMapByPlatformItemIds(platformItemIds);
		Collection<String> skuCodes = new HashSet<>(skuCodeMap.values());
		if (skuCodes.isEmpty()) {
			return Collections.emptyMap();
		}

		List<Sku> skus = skuService.listBySkuCodes(new ArrayList<>(skuCodes));
		return skus.stream()
				.collect(Collectors.toMap(Sku::getSkuCode, s -> s, (a, b) -> a));
	}

	/**
	 * 批量查询 platformItemId → skuCode 映射
	 */
	public Map<String, String> loadSkuCodeMap(List<ErpOrder> orders) {
		if (orders == null || orders.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<String> platformItemIds = orders.stream()
				.filter(o -> o.getItems() != null)
				.flatMap(o -> o.getItems().stream())
				.map(ErpOrderItem::getPlatformItemId)
				.filter(StringUtils::hasText)
				.collect(Collectors.toSet());
		return skuMappingService.getSkuCodeMapByPlatformItemIds(platformItemIds);
	}
}
