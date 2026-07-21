package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.StocktakeLocationTask;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

public interface StocktakeLocationTaskMapper extends ExtendMapper<StocktakeLocationTask> {

	default List<StocktakeLocationTask> listByStocktakeId(Long stocktakeId) {
		return selectList(WrappersX.lambdaQueryX(StocktakeLocationTask.class)
				.eq(StocktakeLocationTask::getStocktakeOrderId, stocktakeId)
				.orderByAsc(StocktakeLocationTask::getLocationCode));
	}

	default int deleteByStocktakeId(Long stocktakeId) {
		return delete(WrappersX.lambdaQueryX(StocktakeLocationTask.class)
				.eq(StocktakeLocationTask::getStocktakeOrderId, stocktakeId));
	}
}
