package com.erp.admin.wms.service.platform;

import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;

public interface FulfillmentPlatformAdapter {
	String sourceType();

	PlatformActionResult accept(WmsFulfillmentOrder order);

	PlatformLabelResult fetchLabel(WmsFulfillmentOrder order);

	PlatformActionResult markReady(WmsFulfillmentOrder order);

	PlatformActionResult finalizeShipment(WmsFulfillmentOrder order);
}

