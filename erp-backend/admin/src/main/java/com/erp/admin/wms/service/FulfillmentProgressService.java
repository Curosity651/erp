package com.erp.admin.wms.service;

import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class FulfillmentProgressService {

	private final ErpOrderMapper erpOrderMapper;

	public void sync(WmsFulfillmentOrder fulfillment, FulfillmentStatus status) {
		Assert.notNull(fulfillment, "履约单不能为空");
		Assert.notNull(status, "履约状态不能为空");
		if ("MANUAL".equals(fulfillment.getSourceType()) || fulfillment.getSourceOrderId() == null) {
			return;
		}
		Assert.isTrue(erpOrderMapper.updateWarehouseFulfillmentStatus(fulfillment.getSourceOrderId(),
				fulfillment.getErpTenantId(), status.name(), fulfillment.getId()) == 1,
				"货主订单仓库状态更新失败");
	}
}
