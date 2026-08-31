package com.erp.admin.wms.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

import com.erp.admin.wms.mapper.WmsClientBillingRecordMapper;
import com.erp.admin.wms.model.entity.WmsClientBillingRecord;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class FulfillmentLogisticsFeeService {

	private static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Shanghai");

	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Moscow");

	private final WmsClientBillingRecordMapper billingRecordMapper;

	public WmsClientBillingRecord record(WmsFulfillmentOrder order) {
		Assert.notNull(order, "履约订单不能为空");
		Assert.notNull(order.getId(), "履约订单ID不能为空");
		Assert.notNull(order.getLogisticsProductId(), "订单未选择物流产品");
		Assert.notNull(order.getLogisticsProductActualFee(), "订单物流产品费用未确定");
		Assert.hasText(order.getLogisticsProductCurrency(), "订单物流产品币种未确定");

		String bizId = "FULFILLMENT_LOGISTICS:" + order.getId();
		WmsClientBillingRecord existing = billingRecordMapper.selectByBizId(bizId);
		if (existing != null) {
			return existing;
		}

		WmsClientBillingRecord record = new WmsClientBillingRecord();
		record.setBizId(bizId);
		record.setWmsTenantId(order.getWmsTenantId());
		record.setErpTenantId(order.getErpTenantId());
		record.setFulfillmentOrderId(order.getId());
		record.setFeeType("SHIPPING");
		record.setAmount(order.getLogisticsProductActualFee());
		record.setCurrency(order.getLogisticsProductCurrency());
		record.setTrackingNo(order.getTrackingNo());
		record.setLogisticsProductId(order.getLogisticsProductId());
		record.setProductNameSnapshot(order.getLogisticsProductName());
		record.setProductDescriptionSnapshot(order.getLogisticsProductDescription());
		record.setBillMonth(resolveBillMonth(order.getShippedTime()));
		billingRecordMapper.insert(record);
		return record;
	}

	public static String resolveBillMonth(LocalDateTime shippedTime) {
		if (shippedTime == null) {
			return YearMonth.now(BUSINESS_ZONE).toString();
		}
		return YearMonth.from(shippedTime.atZone(STORAGE_ZONE).withZoneSameInstant(BUSINESS_ZONE)).toString();
	}

}
