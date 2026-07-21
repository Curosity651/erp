package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.AdjustmentOrderItem;
import com.erp.admin.wms.model.vo.AdjustmentItemVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

/**
 * 调整单明细 Mapper
 *
 * @author erp
 */
public interface AdjustmentItemMapper extends ExtendMapper<AdjustmentOrderItem> {

	/**
	 * 根据调整单ID查询明细列表
	 * @param adjustmentOrderId 调整单ID
	 * @return List<AdjustmentOrderItem> 明细列表
	 */
	default List<AdjustmentOrderItem> selectByAdjustmentOrderId(Long adjustmentOrderId) {
		LambdaQueryWrapperX<AdjustmentOrderItem> wrapper = WrappersX.lambdaQueryX(AdjustmentOrderItem.class)
			.eq(AdjustmentOrderItem::getAdjustmentOrderId, adjustmentOrderId)
			.orderByAsc(AdjustmentOrderItem::getId);
		return this.selectList(wrapper);
	}

	/**
	 * 根据调整单ID删除明细
	 * @param adjustmentOrderId 调整单ID
	 * @return 影响行数
	 */
	default int deleteByAdjustmentOrderId(Long adjustmentOrderId) {
		LambdaQueryWrapperX<AdjustmentOrderItem> wrapper = WrappersX.lambdaQueryX(AdjustmentOrderItem.class)
			.eq(AdjustmentOrderItem::getAdjustmentOrderId, adjustmentOrderId);
		return this.delete(wrapper);
	}

}
