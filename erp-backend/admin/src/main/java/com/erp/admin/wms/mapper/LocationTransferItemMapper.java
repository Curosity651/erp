package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.LocationTransferItem;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

/**
 * 库位调整单明细 Mapper。
 *
 * @author erp
 */
public interface LocationTransferItemMapper extends ExtendMapper<LocationTransferItem> {

	/**
	 * 按调整单ID查询明细。
	 * @param transferOrderId 调整单ID
	 * @return 明细列表
	 */
	default List<LocationTransferItem> selectByOrderId(Long transferOrderId) {
		LambdaQueryWrapperX<LocationTransferItem> wrapper = WrappersX.lambdaQueryX(LocationTransferItem.class)
			.eq(LocationTransferItem::getTransferOrderId, transferOrderId)
			.orderByAsc(LocationTransferItem::getId);
		return this.selectList(wrapper);
	}

	/**
	 * 按调整单ID删除明细。
	 * @param transferOrderId 调整单ID
	 * @return 影响行数
	 */
	default int deleteByOrderId(Long transferOrderId) {
		LambdaQueryWrapperX<LocationTransferItem> wrapper = WrappersX.lambdaQueryX(LocationTransferItem.class)
			.eq(LocationTransferItem::getTransferOrderId, transferOrderId);
		return this.delete(wrapper);
	}

}
