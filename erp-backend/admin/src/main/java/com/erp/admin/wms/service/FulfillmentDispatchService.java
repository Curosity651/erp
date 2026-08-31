package com.erp.admin.wms.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
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

		return createTasks(accepted.getSuccessIds(), operatorId, result);
	}

	public FulfillmentDispatchResultVO redispatch(List<Long> orderIds, Long operatorId) {
		Assert.notEmpty(orderIds, "请选择需要重新派单的订单");
		FulfillmentDispatchResultVO result = new FulfillmentDispatchResultVO();
		List<Long> retryable = new ArrayList<>();
		Map<Long, WmsFulfillmentOrder> orderById = index(orderMapper.selectBatchIds(orderIds));
		for (Long orderId : orderIds) {
			WmsFulfillmentOrder order = orderById.get(orderId);
			if (order == null) {
				result.addFailure(orderId, "履约订单不存在");
			}
			else if (order.getFulfillmentStatus() != FulfillmentStatus.WAITING_PICK) {
				result.addFailure(orderId, "订单不是待拣货状态，不能重新派单");
			}
			else if (pickingService.hasTaskAssociation(orderId)) {
				orderMapper.updateDispatchStatus(orderId, "SUCCEEDED", null);
				result.addSuccess(orderId);
			}
			else {
				retryable.add(orderId);
			}
		}
		return createTasks(retryable, operatorId, result);
	}

	private FulfillmentDispatchResultVO createTasks(List<Long> orderIds, Long operatorId,
			FulfillmentDispatchResultVO result) {
		Map<Long, WmsFulfillmentOrder> orderById = index(orderMapper.selectBatchIds(orderIds));
		Map<Long, List<Long>> warehouseGroups = new LinkedHashMap<>();
		for (Long orderId : orderIds) {
			WmsFulfillmentOrder order = orderById.get(orderId);
			if (order == null || order.getWarehouseId() == null) {
				fail(orderId, "订单仓库不存在", result);
				continue;
			}
			warehouseGroups.computeIfAbsent(order.getWarehouseId(), key -> new ArrayList<>()).add(orderId);
		}

		for (List<Long> warehouseOrderIds : warehouseGroups.values()) {
			warehouseOrderIds.forEach(id -> orderMapper.updateDispatchStatus(id, "PENDING", null));
			try {
				WmsFulfillmentPickTask task = pickingService.createTask(warehouseOrderIds, operatorId);
				result.addTask(task);
				for (Long orderId : warehouseOrderIds) {
					orderMapper.updateDispatchStatus(orderId, "SUCCEEDED", null);
					result.addSuccess(orderId);
				}
			}
			catch (RuntimeException ex) {
				String reason = rootMessage(ex);
				for (Long orderId : warehouseOrderIds) {
					if (pickingService.hasTaskAssociation(orderId)) {
						orderMapper.updateDispatchStatus(orderId, "SUCCEEDED", null);
						result.addSuccess(orderId);
					}
					else {
						fail(orderId, reason, result);
					}
				}
			}
		}
		return result;
	}

	private Map<Long, WmsFulfillmentOrder> index(List<WmsFulfillmentOrder> orders) {
		Map<Long, WmsFulfillmentOrder> result = new HashMap<>();
		if (orders != null) {
			for (WmsFulfillmentOrder order : orders) result.put(order.getId(), order);
		}
		return result;
	}

	private void fail(Long orderId, String reason, FulfillmentDispatchResultVO result) {
		String message = "已下架但派单失败：" + reason;
		orderMapper.updateDispatchStatus(orderId, "FAILED", reason);
		result.addFailure(orderId, message);
	}

	private String rootMessage(RuntimeException ex) {
		Throwable current = ex;
		while (current.getCause() != null) current = current.getCause();
		return current.getMessage() == null ? "未知错误" : current.getMessage();
	}
}
