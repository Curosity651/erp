package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.StocktakeOrderItem;
import com.erp.admin.wms.model.vo.StocktakeItemVO;
import com.erp.admin.wms.model.vo.StocktakeProgressVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

/**
 * 盘点单明细 Mapper
 *
 * @author erp
 */
public interface StocktakeItemMapper extends ExtendMapper<StocktakeOrderItem> {

	/**
	 * 根据盘点单ID查询明细列表
	 * @param stocktakeOrderId 盘点单ID
	 * @return List<StocktakeOrderItem> 明细列表
	 */
	default List<StocktakeOrderItem> selectByStocktakeOrderId(Long stocktakeOrderId) {
		LambdaQueryWrapperX<StocktakeOrderItem> wrapper = WrappersX.lambdaQueryX(StocktakeOrderItem.class)
			.eq(StocktakeOrderItem::getStocktakeOrderId, stocktakeOrderId)
			.orderByAsc(StocktakeOrderItem::getId);
		return this.selectList(wrapper);
	}

	default List<StocktakeOrderItem> selectByTaskId(Long taskId) {
		return this.selectList(WrappersX.lambdaQueryX(StocktakeOrderItem.class)
				.eq(StocktakeOrderItem::getLocationTaskId, taskId)
				.orderByAsc(StocktakeOrderItem::getSkuCode)
				.orderByAsc(StocktakeOrderItem::getId));
	}

	/**
	 * 根据盘点单ID删除明细
	 * @param stocktakeOrderId 盘点单ID
	 * @return 影响行数
	 */
	default int deleteByStocktakeOrderId(Long stocktakeOrderId) {
		LambdaQueryWrapperX<StocktakeOrderItem> wrapper = WrappersX.lambdaQueryX(StocktakeOrderItem.class)
			.eq(StocktakeOrderItem::getStocktakeOrderId, stocktakeOrderId);
		return this.delete(wrapper);
	}

	/**
	 * 查询盘点进度统计
	 * @param stocktakeOrderId 盘点单ID
	 * @return StocktakeProgressVO 盘点进度
	 */
	StocktakeProgressVO selectProgress(@Param("stocktakeOrderId") Long stocktakeOrderId);

	/**
	 * 查询有差异的明细列表
	 * @param stocktakeOrderId 盘点单ID
	 * @return List<StocktakeOrderItem> 有差异的明细列表
	 */
	default List<StocktakeOrderItem> selectDiffItems(Long stocktakeOrderId) {
		LambdaQueryWrapperX<StocktakeOrderItem> wrapper = WrappersX.lambdaQueryX(StocktakeOrderItem.class)
			.eq(StocktakeOrderItem::getStocktakeOrderId, stocktakeOrderId)
			.isNotNull(StocktakeOrderItem::getDiffQuantity)
			.ne(StocktakeOrderItem::getDiffQuantity, 0)
			.orderByAsc(StocktakeOrderItem::getId);
		return this.selectList(wrapper);
	}

}
