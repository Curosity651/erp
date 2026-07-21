package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.PurchaseOrderItem;
import com.erp.admin.wms.model.param.QuantityUpdateParam;
import com.erp.admin.wms.model.vo.PurchaseOrderStatVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.Collection;
import java.util.List;

/**
 * 采购单明细 Mapper
 *
 * @author erp
 */
public interface PurchaseOrderItemMapper extends ExtendMapper<PurchaseOrderItem> {

	/**
	 * 批量统计采购单明细数据
	 * @param orderIds 采购单ID列表
	 * @return 统计结果列表
	 */
	List<PurchaseOrderStatVO> selectStatByOrderIds(@Param("orderIds") Collection<Long> orderIds);

	/**
	 * 根据采购单ID查询明细列表
	 * @param purchaseOrderId 采购单ID
	 * @return 明细列表
	 */
	default List<PurchaseOrderItem> selectByPurchaseOrderId(Long purchaseOrderId) {
		LambdaQueryWrapperX<PurchaseOrderItem> wrapper = WrappersX.lambdaQueryX(PurchaseOrderItem.class)
				.eq(PurchaseOrderItem::getPurchaseOrderId, purchaseOrderId)
				.orderByAsc(PurchaseOrderItem::getId);
		return this.selectList(wrapper);
	}

	/**
	 * 根据采购单ID删除明细
	 * @param purchaseOrderId 采购单ID
	 * @return 删除数量
	 */
	default int deleteByPurchaseOrderId(Long purchaseOrderId) {
		LambdaQueryWrapperX<PurchaseOrderItem> wrapper = WrappersX.lambdaQueryX(PurchaseOrderItem.class)
				.eq(PurchaseOrderItem::getPurchaseOrderId, purchaseOrderId);
		return this.delete(wrapper);
	}

	/**
	 * 批量增加已入库数量（入库单确认时）
	 * @param items 更新参数列表
	 * @return 影响行数
	 */
	int batchIncreaseReceivedQuantity(@Param("items") List<QuantityUpdateParam> items);

	/**
	 * 增加已发货数量（确认发货时）
	 * @param itemId 采购单明细ID
	 * @param quantity 数量
	 * @return 影响行数（0表示可发货数量不足）
	 */
	int increaseShippedQuantity(@Param("itemId") Long itemId, @Param("quantity") Integer quantity);

	/**
	 * 增加已入库数量（单条）
	 * @param itemId 采购单明细ID
	 * @param quantity 数量
	 * @return 影响行数
	 */
	int increaseReceivedQuantity(@Param("itemId") Long itemId, @Param("quantity") Integer quantity);

}
