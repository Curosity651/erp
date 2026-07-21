package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.ShippingOrderItem;
import com.erp.admin.wms.model.dto.ShippingAllocationRowDTO;
import com.erp.admin.wms.model.vo.ShippingOrderItemVO;
import com.erp.admin.wms.model.vo.ShippingOrderSkuCountVO;
import com.erp.admin.wms.model.vo.ShippingQuantityStatsVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import com.erp.admin.wms.model.param.ReceivedQuantityUpdateParam;
import com.erp.admin.wms.model.vo.ShippingOrderArrivalStatsVO;

import java.util.List;

/**
 * 物流单明细 Mapper
 *
 * @author erp
 */
public interface ShippingOrderItemMapper extends ExtendMapper<ShippingOrderItem> {

	/**
	 * 根据物流单ID查询明细列表
	 * @param shippingOrderId 物流单ID
	 * @return List<ShippingOrderItem> 明细列表
	 */
	default List<ShippingOrderItem> selectByShippingOrderId(Long shippingOrderId) {
		LambdaQueryWrapperX<ShippingOrderItem> wrapper = WrappersX.lambdaQueryX(ShippingOrderItem.class)
			.eq(ShippingOrderItem::getShippingOrderId, shippingOrderId)
			.orderByAsc(ShippingOrderItem::getId);
		return this.selectList(wrapper);
	}

	/**
	 * 根据物流单ID删除明细
	 * @param shippingOrderId 物流单ID
	 * @return 影响行数
	 */
	default int deleteByShippingOrderId(Long shippingOrderId) {
		LambdaQueryWrapperX<ShippingOrderItem> wrapper = WrappersX.lambdaQueryX(ShippingOrderItem.class)
			.eq(ShippingOrderItem::getShippingOrderId, shippingOrderId);
		return this.delete(wrapper);
	}

	/**
	 * 查询物流单明细VO列表（含采购单号、SKU名称）
	 * @param shippingOrderId 物流单ID
	 * @return List<ShippingOrderItemVO> 明细VO列表
	 */
	List<ShippingOrderItemVO> selectItemVOsByShippingOrderId(@Param("shippingOrderId") Long shippingOrderId);

	/**
	 * 批量查询物流单的SKU种类数
	 * @param shippingOrderIds 物流单ID列表
	 * @return List<ShippingOrderSkuCountVO> SKU种类数列表
	 */
	List<ShippingOrderSkuCountVO> selectSkuCountByShippingOrderIds(@Param("shippingOrderIds") List<Long> shippingOrderIds);

	/**
	 * 批量查询物流单的发货/入库数量统计
	 * @param shippingOrderIds 物流单ID列表
	 * @return List<ShippingQuantityStatsVO> 数量统计列表
	 */
	List<ShippingQuantityStatsVO> selectQuantityStatsByShippingOrderIds(@Param("shippingOrderIds") List<Long> shippingOrderIds);

	/**
	 * 批量增加已到货数量
	 * @param params 更新参数列表（shippingOrderItemId, quantity）
	 * @return 更新行数
	 */
	int batchIncreaseReceivedQuantity(@Param("params") List<ReceivedQuantityUpdateParam> params);

	/**
	 * 查询物流单的到货统计
	 * @param shippingOrderId 物流单ID
	 * @return 到货统计VO
	 */
	ShippingOrderArrivalStatsVO selectArrivalStats(@Param("shippingOrderId") Long shippingOrderId);

	/**
	 * 增加已到货数量（入库单确认时）
	 * @param itemId 物流单明细ID
	 * @param quantity 实际到货数量
	 * @return 影响行数（0表示可入库数量不足）
	 */
	int increaseReceivedQuantity(@Param("itemId") Long itemId, @Param("quantity") Integer quantity);

	int reserveInboundQuantity(@Param("itemId") Long itemId, @Param("quantity") Integer quantity);

	int settleInboundReservation(@Param("itemId") Long itemId,
			@Param("reservedQuantity") Integer reservedQuantity,
			@Param("actualQuantity") Integer actualQuantity);

	/**
	 * 按固定顺序锁定采购明细，串行化付款占用与确认发货。
	 */
	List<Long> lockPurchaseOrderItems(@Param("purchaseOrderItemIds") List<Long> purchaseOrderItemIds);

	/**
	 * 查询采购明细的已发货数量和其他待发货草稿占用。
	 */
	List<ShippingAllocationRowDTO> selectShippingAllocationRows(
			@Param("shippingOrderId") Long shippingOrderId,
			@Param("purchaseOrderItemIds") List<Long> purchaseOrderItemIds);

}
