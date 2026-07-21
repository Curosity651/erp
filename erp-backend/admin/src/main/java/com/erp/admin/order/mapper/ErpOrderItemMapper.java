package com.erp.admin.order.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.order.model.entity.ErpOrderItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单商品明细 Mapper
 *
 * @author system
 */
public interface ErpOrderItemMapper extends ExtendMapper<ErpOrderItem> {

	/**
	 * 按订单ID查询商品明细
	 */
	default List<ErpOrderItem> selectByOrderId(Long orderId) {
		return this.selectList(Wrappers.lambdaQuery(ErpOrderItem.class)
				.eq(ErpOrderItem::getOrderId, orderId));
	}

	/**
	 * 按订单ID批量查询商品明细（避免 N+1）
	 */
	default List<ErpOrderItem> selectByOrderIds(List<Long> orderIds) {
		if (orderIds == null || orderIds.isEmpty()) {
			return Collections.emptyList();
		}
		return this.selectList(Wrappers.lambdaQuery(ErpOrderItem.class)
				.in(ErpOrderItem::getOrderId, orderIds));
	}

	/**
	 * 按订单ID删除所有商品明细（先删后插场景）
	 */
	default int deleteByOrderId(Long orderId) {
		return this.delete(Wrappers.lambdaQuery(ErpOrderItem.class)
				.eq(ErpOrderItem::getOrderId, orderId));
	}

	/**
	 * 按 ID 更新平台同步字段（保留 returnedQuantity / createTime 等 ERP 内部字段）
	 * <p>
	 * 使用 LambdaUpdateWrapper 显式 SET，确保字段值为 null 时也能正确写入。
	 * 不更新的字段：id, orderId, platformItemId, returnedQuantity, createTime
	 */
	default int updatePlatformFields(ErpOrderItem item) {
		return this.update(null, Wrappers.lambdaUpdate(ErpOrderItem.class)
				.eq(ErpOrderItem::getId, item.getId())
				.set(ErpOrderItem::getQuantity, item.getQuantity())
				.set(ErpOrderItem::getItemPrice, item.getItemPrice())
				.set(ErpOrderItem::getItemAmount, item.getItemAmount())
				.set(ErpOrderItem::getItemAmountRub, item.getItemAmountRub())
				.set(ErpOrderItem::getUpdateTime, item.getUpdateTime()));
	}

	/**
	 * 更新 item 的已退货数量（原子增量）
	 *
	 * @param itemId   erp_order_item.id
	 * @param quantity 退货数量（增量）
	 * @return 影响行数
	 */
	@Update("UPDATE erp_order_item " +
			"SET returned_quantity = COALESCE(returned_quantity, 0) + #{quantity} " +
			"WHERE id = #{itemId} " +
			"AND COALESCE(returned_quantity, 0) + #{quantity} <= quantity")
	int updateReturnedQuantity(@Param("itemId") Long itemId, @Param("quantity") Integer quantity);

	/**
	 * 按 platformItemId 列表查询关联的订单ID列表
	 */
	default List<Long> selectOrderIdsByPlatformItemIds(List<String> platformItemIds) {
		if (platformItemIds == null || platformItemIds.isEmpty()) {
			return Collections.emptyList();
		}
		List<ErpOrderItem> items = this.selectList(
				Wrappers.lambdaQuery(ErpOrderItem.class)
						.select(ErpOrderItem::getOrderId)
						.in(ErpOrderItem::getPlatformItemId, platformItemIds));
		return items.stream()
				.map(ErpOrderItem::getOrderId)
				.distinct()
				.collect(Collectors.toList());
	}

}
