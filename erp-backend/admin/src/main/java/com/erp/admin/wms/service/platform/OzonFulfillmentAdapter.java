package com.erp.admin.wms.service.platform;

import java.util.Collections;

import com.erp.admin.order.service.label.LabelPrintOrchestrator;
import com.erp.admin.order.service.ozon.OzonOrderConfirmService;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OzonFulfillmentAdapter extends PlatformAdapterSupport implements FulfillmentPlatformAdapter {
	private final OzonOrderConfirmService confirmService;
	private final LabelPrintOrchestrator labelOrchestrator;

	@Override public String sourceType() { return "OZON"; }
	@Override public PlatformActionResult accept(WmsFulfillmentOrder order) {
		return result(confirmService.confirmOrders(Collections.singletonList(order.getSourceOrderId())),
				order.getSourceOrderId());
	}
	@Override public PlatformLabelResult fetchLabel(WmsFulfillmentOrder order) {
		return printLabel(PlatformEnum.Ozon.code(), order, labelOrchestrator);
	}
	@Override public PlatformActionResult markReady(WmsFulfillmentOrder order) {
		return PlatformActionResult.success("Ozon 包裹已处于待交付状态", order.getSourceOrderNo());
	}
	@Override public PlatformActionResult finalizeShipment(WmsFulfillmentOrder order) {
		return PlatformActionResult.success("Ozon 仓库签出完成", order.getSourceOrderNo());
	}
}

