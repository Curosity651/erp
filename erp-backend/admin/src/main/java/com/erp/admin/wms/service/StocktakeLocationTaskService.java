package com.erp.admin.wms.service;

import com.erp.admin.wms.mapper.StocktakeLocationTaskMapper;
import com.erp.admin.wms.model.entity.StocktakeLocationTask;
import com.erp.admin.wms.model.enums.StocktakeTaskStatus;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StocktakeLocationTaskService
		extends ExtendServiceImpl<StocktakeLocationTaskMapper, StocktakeLocationTask> {

	public List<StocktakeLocationTask> listByStocktakeId(Long stocktakeId) {
		return baseMapper.listByStocktakeId(stocktakeId);
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
