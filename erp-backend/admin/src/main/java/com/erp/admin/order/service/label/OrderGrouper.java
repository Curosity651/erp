package com.erp.admin.order.service.label;

import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.product.model.entity.Sku;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单分组组件
 * <p>
 * 职责：
 * - 按仓库和 SKU 分组订单
 * - 构建分组键
 * <p>
 * 使用场景：WB 和 Ozon 面单打印服务
 *
 * @author system
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderGrouper {

	/**
	 * 按仓库和 SKU 分组订单
	 *
	 * @param orders 订单列表
	 * @param skuMap SKU 映射
	 * @return 分组键 -> 订单分组
	 */
	public Map<String, OrderGroup> groupByWarehouseAndSku(
			List<ErpOrder> orders,
			Map<String, Sku> skuMap,
			Map<String, String> skuCodeMap) {

		if (orders == null || orders.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, OrderGroup> groups = new LinkedHashMap<>();

		for (ErpOrder order : orders) {
			String destinationWarehouseId = order.getDestinationWarehouseId();

			// 从首个 item 的 platformItemId 查 skuCode 用于分组
			List<ErpOrderItem> items = order.getItems();
			String skuCode;
			Sku sku;
			String platformItemId = (items != null && !items.isEmpty())
					? items.get(0).getPlatformItemId() : null;
			if (StringUtils.hasText(platformItemId)
					&& skuCodeMap.containsKey(platformItemId)) {
				skuCode = skuCodeMap.get(platformItemId);
				sku = skuMap.get(skuCode);
			} else {
				skuCode = "";
				sku = null;
			}

			String groupKey = buildGroupKey(destinationWarehouseId, skuCode);

			OrderGroup group = groups.computeIfAbsent(groupKey, k -> new OrderGroup());
			group.setDestinationWarehouseId(destinationWarehouseId);
			group.setSku(sku);
			group.getOrders().add(order);
		}

		return groups;
	}

	/**
	 * 构建分组键
	 *
	 * @param warehouseId 仓库ID
	 * @param skuCode     SKU代码
	 * @return 分组键
	 */
	public String buildGroupKey(String warehouseId, String skuCode) {
		String w = warehouseId == null ? "" : warehouseId;
		String s = skuCode == null ? "" : skuCode;
		return w + "#" + s;
	}

	/**
	 * 订单分组数据结构
	 */
	@Data
	public static class OrderGroup {
		private String destinationWarehouseId;
		private String destinationWarehouseName;
		private Sku sku;
		private List<ErpOrder> orders = new ArrayList<>();
	}
}
