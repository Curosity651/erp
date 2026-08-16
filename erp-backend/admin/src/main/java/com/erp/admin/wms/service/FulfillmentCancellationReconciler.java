package com.erp.admin.wms.service;

import java.util.List;
import java.util.Locale;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FulfillmentCancellationReconciler {
	private final WmsFulfillmentOrderMapper fulfillmentMapper;
	private final ErpOrderMapper erpOrderMapper;
	private final FulfillmentStatusSyncService statusSyncService;

	@Scheduled(fixedDelay = 60000L, initialDelay = 30000L)
	public void reconcile() {
		List<WmsFulfillmentOrder> active = fulfillmentMapper.selectList(
				Wrappers.<WmsFulfillmentOrder>lambdaQuery()
						.notIn(WmsFulfillmentOrder::getFulfillmentStatus,
								FulfillmentStatus.DRAFT, FulfillmentStatus.CANCELLED,
								FulfillmentStatus.CANCEL_RETURNING, FulfillmentStatus.SHIPPED)
						.ne(WmsFulfillmentOrder::getSourceType, "MANUAL"));
		for (WmsFulfillmentOrder fulfillment : active) {
			try {
				ErpOrder order = erpOrderMapper.selectById(fulfillment.getSourceOrderId());
				if (order != null && cancelled(order)) {
					statusSyncService.platformCancelled(fulfillment.getSourceType(),
							fulfillment.getSourceOrderId(), "平台订单已取消");
				}
			}
			catch (RuntimeException ex) {
				log.warn("履约订单取消对账失败, fulfillmentId={}", fulfillment.getId(), ex);
			}
		}
	}

	private boolean cancelled(ErpOrder order) {
		String value = ((order.getPlatformStatus() == null ? "" : order.getPlatformStatus()) + " "
				+ (order.getErpStatus() == null ? "" : order.getErpStatus()))
				.toLowerCase(Locale.ROOT);
		return value.contains("cancel") || value.contains("取消");
	}
}
