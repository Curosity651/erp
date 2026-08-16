package com.erp.admin.wms.service;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPlatformActionMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentPlatformAction;
import com.erp.admin.wms.service.platform.FulfillmentPlatformAdapter;
import com.erp.admin.wms.service.platform.PlatformActionResult;
import com.erp.admin.wms.service.platform.PlatformLabelResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
public class FulfillmentPlatformActionService {
	private final WmsFulfillmentOrderMapper orderMapper;
	private final WmsFulfillmentPlatformActionMapper actionMapper;
	private final Map<String, FulfillmentPlatformAdapter> adapters = new HashMap<>();
	private final ObjectMapper objectMapper;

	public FulfillmentPlatformActionService(WmsFulfillmentOrderMapper orderMapper,
			WmsFulfillmentPlatformActionMapper actionMapper,
			List<FulfillmentPlatformAdapter> adapterList, ObjectMapper objectMapper) {
		this.orderMapper = orderMapper;
		this.actionMapper = actionMapper;
		this.objectMapper = objectMapper;
		for (FulfillmentPlatformAdapter adapter : adapterList) {
			adapters.put(adapter.sourceType().toUpperCase(Locale.ROOT), adapter);
		}
	}

	@Transactional(noRollbackFor = RuntimeException.class)
	public PlatformActionResult accept(Long fulfillmentOrderId) {
		return execute(fulfillmentOrderId, "ACCEPT", PlatformActionResult.class,
				adapter -> adapter.accept(requireOrder(fulfillmentOrderId)));
	}

	@Transactional(noRollbackFor = RuntimeException.class)
	public PlatformLabelResult fetchLabel(Long fulfillmentOrderId) {
		return execute(fulfillmentOrderId, "FETCH_LABEL", PlatformLabelResult.class,
				adapter -> adapter.fetchLabel(requireOrder(fulfillmentOrderId)));
	}

	@Transactional(noRollbackFor = RuntimeException.class)
	public PlatformActionResult markReady(Long fulfillmentOrderId) {
		return execute(fulfillmentOrderId, "MARK_READY", PlatformActionResult.class,
				adapter -> adapter.markReady(requireOrder(fulfillmentOrderId)));
	}

	@Transactional(noRollbackFor = RuntimeException.class)
	public PlatformActionResult finalizeShipment(Long fulfillmentOrderId) {
		return execute(fulfillmentOrderId, "FINALIZE", PlatformActionResult.class,
				adapter -> adapter.finalizeShipment(requireOrder(fulfillmentOrderId)));
	}

	protected <T extends PlatformActionResult> T execute(Long fulfillmentOrderId, String actionType,
			Class<T> resultType, Function<FulfillmentPlatformAdapter, T> invocation) {
		WmsFulfillmentOrder order = requireOrder(fulfillmentOrderId);
		FulfillmentPlatformAdapter adapter = adapters.get(order.getSourceType().toUpperCase(Locale.ROOT));
		Assert.notNull(adapter, "不支持的履约来源：" + order.getSourceType());
		WmsFulfillmentPlatformAction action = actionMapper.selectForUpdate(fulfillmentOrderId, actionType);
		if (action != null && "SUCCEEDED".equals(action.getActionStatus())) {
			return readResult(action.getResponsePayload(), resultType);
		}
		if (action != null && ("STARTED".equals(action.getActionStatus())
				|| "RECONCILE_REQUIRED".equals(action.getActionStatus()))) {
			throw new IllegalStateException("平台动作正在执行或需要先对账，请勿重复提交");
		}
		if (action == null) {
			action = new WmsFulfillmentPlatformAction();
			action.setFulfillmentOrderId(fulfillmentOrderId);
			action.setActionType(actionType);
			action.setAttemptCount(1);
			action.setRequestFingerprint(fingerprint(order, actionType));
			action.setRequestPayload(write(order));
			action.setActionStatus("STARTED");
			action.setStartedTime(LocalDateTime.now());
			Assert.isTrue(actionMapper.insert(action) == 1, "平台动作登记失败");
		}
		else {
			action.setActionStatus("STARTED");
			action.setErrorMessage(null);
			action.setStartedTime(LocalDateTime.now());
			action.setAttemptCount((action.getAttemptCount() == null ? 0 : action.getAttemptCount()) + 1);
			actionMapper.updateById(action);
		}
		try {
			T result = invocation.apply(adapter);
			Assert.notNull(result, "平台动作返回为空");
			action.setResponsePayload(write(result));
			action.setActionStatus(result.isSuccess() ? "SUCCEEDED" : "FAILED");
			action.setErrorMessage(result.isSuccess() ? null : result.getMessage());
			action.setCompletedTime(LocalDateTime.now());
			actionMapper.updateById(action);
			if (!result.isSuccess()) {
				throw new IllegalStateException(result.getMessage());
			}
			return result;
		}
		catch (RuntimeException ex) {
			action.setActionStatus(isTimeout(ex) ? "RECONCILE_REQUIRED" : "FAILED");
			action.setErrorMessage(ex.getMessage());
			action.setCompletedTime(LocalDateTime.now());
			actionMapper.updateById(action);
			throw ex;
		}
	}

	private WmsFulfillmentOrder requireOrder(Long id) {
		WmsFulfillmentOrder order = orderMapper.selectById(id);
		Assert.notNull(order, "履约订单不存在");
		Assert.hasText(order.getSourceType(), "履约来源为空");
		return order;
	}

	private String fingerprint(WmsFulfillmentOrder order, String actionType) {
		try {
			String raw = order.getId() + ":" + actionType + ":" + order.getSourceType() + ":" + order.getVersion();
			byte[] bytes = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder result = new StringBuilder();
			for (byte value : bytes) result.append(String.format("%02x", value));
			return result.toString();
		}
		catch (Exception ex) {
			throw new IllegalStateException("请求指纹生成失败", ex);
		}
	}

	private String write(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (Exception ex) {
			throw new IllegalStateException("平台动作数据序列化失败", ex);
		}
	}

	private <T> T readResult(String json, Class<T> type) {
		try {
			return objectMapper.readValue(json, type);
		}
		catch (Exception ex) {
			throw new IllegalStateException("平台动作历史结果解析失败", ex);
		}
	}

	private boolean isTimeout(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof SocketTimeoutException) return true;
			current = current.getCause();
		}
		return false;
	}
}
