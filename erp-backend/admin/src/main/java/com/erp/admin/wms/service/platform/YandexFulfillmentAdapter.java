package com.erp.admin.wms.service.platform;

import java.util.Collections;

import com.erp.admin.order.service.label.LabelPrintOrchestrator;
import com.erp.admin.order.service.yandex.YdOrderConfirmService;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class YandexFulfillmentAdapter extends PlatformAdapterSupport implements FulfillmentPlatformAdapter {
	private final YdOrderConfirmService confirmService;
	private final LabelPrintOrchestrator labelOrchestrator;

	@Override public String sourceType() { return "YANDEX"; }
	@Override public PlatformActionResult accept(WmsFulfillmentOrder order) {
		return PlatformActionResult.success("Yandex 订单已由仓库接单", order.getSourceOrderNo());
	}
	@Override public PlatformLabelResult fetchLabel(WmsFulfillmentOrder order) {
		return printLabel(PlatformEnum.Yandex.code(), order, labelOrchestrator);
	}
	@Override public PlatformActionResult markReady(WmsFulfillmentOrder order) {
		return result(confirmService.confirmOrders(Collections.singletonList(order.getSourceOrderId())),
				order.getSourceOrderId());
	}
	@Override public PlatformActionResult finalizeShipment(WmsFulfillmentOrder order) {
		return PlatformActionResult.success("Yandex 仓库签出完成", order.getSourceOrderNo());
	}
}

