package com.erp.admin.wms.model.vo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

@Getter
public class FulfillmentBatchResultVO {
	private final List<Long> successIds = new ArrayList<>();
	private final Map<Long, String> failures = new LinkedHashMap<>();

	public void addSuccess(Long id) {
		successIds.add(id);
	}

	public void addFailure(Long id, String message) {
		failures.put(id, message);
	}
}
