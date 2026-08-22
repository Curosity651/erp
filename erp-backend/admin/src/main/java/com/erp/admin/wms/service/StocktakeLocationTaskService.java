package com.erp.admin.wms.service;

import com.erp.admin.wms.mapper.StocktakeLocationTaskMapper;
import com.erp.admin.wms.model.entity.StocktakeLocationTask;
import com.erp.admin.wms.model.enums.StocktakeTaskStatus;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class StocktakeLocationTaskService
		extends ExtendServiceImpl<StocktakeLocationTaskMapper, StocktakeLocationTask> {

	public List<StocktakeLocationTask> listByStocktakeId(Long stocktakeId) {
		return baseMapper.listByStocktakeId(stocktakeId);
	}

	public List<StocktakeLocationTask> listByStocktakeIds(Collection<Long> stocktakeIds) {
		if (stocktakeIds == null || stocktakeIds.isEmpty()) {
			return Collections.emptyList();
		}
		return baseMapper.selectList(WrappersX.lambdaQueryX(StocktakeLocationTask.class)
				.in(StocktakeLocationTask::getStocktakeOrderId, stocktakeIds));
	}

	public boolean allCompleted(Long stocktakeId) {
		List<StocktakeLocationTask> tasks = listByStocktakeId(stocktakeId);
		return !tasks.isEmpty() && tasks.stream().allMatch(task ->
				StocktakeTaskStatus.COMPLETED.name().equals(task.getTaskStatus())
						|| StocktakeTaskStatus.REVIEWED.name().equals(task.getTaskStatus()));
	}

	public int deleteByStocktakeId(Long stocktakeId) {
		return baseMapper.deleteByStocktakeId(stocktakeId);
	}
}
