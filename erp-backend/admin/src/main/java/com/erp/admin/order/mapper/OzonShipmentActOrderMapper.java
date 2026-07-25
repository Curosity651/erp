package com.erp.admin.order.mapper;

import com.erp.admin.order.model.entity.OzonShipmentActOrder;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

/**
 * Ozon 运单-订单关联 Mapper（留痕）。
 *
 * @author system
 */
public interface OzonShipmentActOrderMapper extends ExtendMapper<OzonShipmentActOrder> {
	default List<OzonShipmentActOrder> selectByOrderId(Long orderId) {
		return selectList(WrappersX.<OzonShipmentActOrder>lambdaQueryX()
				.eq(OzonShipmentActOrder::getOrderId, orderId));
	}
}
