package com.erp.admin.wms.service.platform;

import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import org.springframework.stereotype.Component;

@Component
public class ManualFulfillmentAdapter implements FulfillmentPlatformAdapter {
	@Override public String sourceType() { return "MANUAL"; }
	@Override public PlatformActionResult accept(WmsFulfillmentOrder order) {
		return PlatformActionResult.success("人工订单已接单", order.getSourceOrderNo());
	}
	@Override public PlatformLabelResult fetchLabel(WmsFulfillmentOrder order) {
		return PlatformLabelResult.success(order.getSourceOrderNo(),
				"/wms/fulfillment/" + order.getId() + "/manual-label", order.getSourceOrderNo());
	}
	@Override public PlatformActionResult markReady(WmsFulfillmentOrder order) {
		return PlatformActionResult.success("人工订单面单已核验", order.getSourceOrderNo());
	}
	@Override public PlatformActionResult finalizeShipment(WmsFulfillmentOrder order) {
		return PlatformActionResult.success("人工订单已签出", order.getSourceOrderNo());
	}
}

