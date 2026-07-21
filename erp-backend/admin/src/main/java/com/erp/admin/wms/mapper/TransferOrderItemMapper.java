package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.TransferOrderItem;
import com.erp.admin.wms.model.vo.TransferOrderStatsVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.Collection;
import java.util.List;

/**
 * 调拨单明细 Mapper
 *
 * @author erp
 */
public interface TransferOrderItemMapper extends ExtendMapper<TransferOrderItem> {

	/**
	 * 根据调拨单ID查询明细列表
	 * @param transferOrderId 调拨单ID
	 * @return List<TransferOrderItem> 明细列表
	 */
	default List<TransferOrderItem> selectByTransferOrderId(Long transferOrderId) {
		LambdaQueryWrapperX<TransferOrderItem> wrapper = WrappersX.lambdaQueryX(TransferOrderItem.class)
			.eq(TransferOrderItem::getTransferOrderId, transferOrderId)
			.orderByAsc(TransferOrderItem::getId);
		return this.selectList(wrapper);
	}

	/**
	 * 根据调拨单ID删除明细
	 * @param transferOrderId 调拨单ID
	 * @return 影响行数
	 */
	default int deleteByTransferOrderId(Long transferOrderId) {
		LambdaQueryWrapperX<TransferOrderItem> wrapper = WrappersX.lambdaQueryX(TransferOrderItem.class)
			.eq(TransferOrderItem::getTransferOrderId, transferOrderId);
		return this.delete(wrapper);
	}

	/**
	 * 批量查询调拨单商品摘要（XML 实现）
	 * @param ids 调拨单ID集合
	 * @return 商品摘要列表
	 */
	List<TransferOrderStatsVO> selectItemSummaryByIds(@Param("ids") Collection<Long> ids);

}
