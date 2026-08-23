package com.erp.admin.wms.model.vo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import lombok.Getter;

@Getter
public class FulfillmentDispatchResultVO {
	private final List<Long> successIds = new ArrayList<>();
	private final Map<Long, String> failures = new LinkedHashMap<>();
	private final List<FulfillmentCreatedTaskVO> tasks = new ArrayList<>();

	public void addSuccess(Long id) {
		successIds.add(id);
	}

	public void addFailure(Long id, String message) {
		failures.put(id, message);
	}

	public void addTask(WmsFulfillmentPickTask task) {
		tasks.add(new FulfillmentCreatedTaskVO(task));
	}
}
