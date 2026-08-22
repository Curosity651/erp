package com.erp.admin.wms;

import java.math.BigDecimal;

import com.erp.admin.wms.mapper.WmsClientBillingRecordMapper;
import com.erp.admin.wms.model.entity.WmsClientBillingRecord;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.service.FulfillmentLogisticsFeeService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FulfillmentLogisticsFeeServiceTest {

	@Test
	void product_charge_uses_snapshot_and_is_idempotent() {
		WmsClientBillingRecordMapper mapper = mock(WmsClientBillingRecordMapper.class);
		FulfillmentLogisticsFeeService service = new FulfillmentLogisticsFeeService(mapper);
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(10L);
		order.setWmsTenantId(2L);
		order.setErpTenantId(3L);
		order.setLogisticsProductId(7L);
		order.setLogisticsProductActualFee(new BigDecimal("35.00"));
		order.setLogisticsProductCurrency("RUB");
		order.setTrackingNo("TRACK-10");

		WmsClientBillingRecord created = service.record(order);
		when(mapper.selectByBizId("FULFILLMENT_LOGISTICS:10")).thenReturn(created);
		WmsClientBillingRecord repeated = service.record(order);

		assertThat(created.getAmount()).isEqualByComparingTo("35.00");
		assertThat(created.getWmsTenantId()).isEqualTo(2L);
		assertThat(repeated).isSameAs(created);
		verify(mapper).insert(any(WmsClientBillingRecord.class));
	}
}
