package com.erp.admin.wms.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import com.erp.admin.wms.model.vo.FulfillmentBatchResultVO;
import com.erp.admin.wms.model.vo.FulfillmentDispatchResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class FulfillmentDispatchService {
	private final FulfillmentPickingService pickingService;
	private final WmsFulfillmentOrderMapper orderMapper;

	public FulfillmentDispatchResultVO dispatch(List<Long> orderIds, Long operatorId) {
		Assert.notEmpty(orderIds, "请选择待下架订单");
		FulfillmentBatchResultVO accepted = pickingService.accept(orderIds);
		FulfillmentDispatchResultVO result = new FulfillmentDispatchResultVO();
		accepted.getFailures().forEach(result::addFailure);
		if (accepted.getSuccessIds().isEmpty()) return result;

		Map<Long, WmsFulfillmentOrder> orderById = new HashMap<>();
		for (WmsFulfillmentOrder order : orderMapper.selectBatchIds(accepted.getSuccessIds())) {
			orderById.put(order.getId(), order);
		}
		Map<Long, List<Long>> warehouseGroups = new LinkedHashMap<>();
		for (Long orderId : accepted.getSuccessIds()) {
			WmsFulfillmentOrder order = orderById.get(orderId);
			if (order == null || order.getWarehouseId() == null) {
				result.addFailure(orderId, "已下架但派单失败：订单仓库不存在");
				continue;
			}
			warehouseGroups.computeIfAbsent(order.getWarehouseId(), key -> new ArrayList<>()).add(orderId);
		}

		for (List<Long> warehouseOrderIds : warehouseGroups.values()) {
			try {
				WmsFulfillmentPickTask task = pickingService.createTask(warehouseOrderIds, operatorId);
				result.addTask(task);
				warehouseOrderIds.forEach(result::addSuccess);
			}
			catch (RuntimeException ex) {
				String reason = rootMessage(ex);
				for (Long orderId : warehouseOrderIds) {
					result.addFailure(orderId, "已下架但派单失败：" + reason);
				}
			}
		}
		return result;
	}

	private String rootMessage(RuntimeException ex) {
		Throwable current = ex;
		while (current.getCause() != null) current = current.getCause();
		return current.getMessage() == null ? "未知错误" : current.getMessage();
	}
}
