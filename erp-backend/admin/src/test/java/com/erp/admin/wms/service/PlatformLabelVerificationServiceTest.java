package com.erp.admin.wms.service;

import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.service.ErpOrderService;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsSalesOutboundPackage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformLabelVerificationServiceTest {

	@Test
	void acceptsPersistedPlatformBarcodeAndKeepsOrderNumberFallback() throws Exception {
		ErpOrderService orderService = mock(ErpOrderService.class);
		ObjectMapper objectMapper = new ObjectMapper();
		PlatformLabelVerificationService service =
				new PlatformLabelVerificationService(orderService, objectMapper);

		ErpOrder order = new ErpOrder();
		order.setId(20L);
		order.setPlatformOrderId("WB-ORDER-20");
		order.setLabelVerifyCodes(objectMapper.writeValueAsString(
				Arrays.asList("WB-REAL-BARCODE", "WB-PARTA-PARTB")));
		when(orderService.getById(20L)).thenReturn(order);

		WmsSalesOutboundPackage pack = new WmsSalesOutboundPackage();
		pack.setErpOrderId(20L);
		pack.setPlatformOrderId("WB-ORDER-20");

		assertThat(service.matches(pack, " WB-REAL-BARCODE ")).isTrue();
		assertThat(service.matches(pack, "WB-ORDER-20")).isTrue();
		assertThat(service.matches(pack, "OTHER")).isFalse();
	}

	@Test
	void acceptsRealPlatformBarcodeForFulfillmentOrder() throws Exception {
		ErpOrderService orderService = mock(ErpOrderService.class);
		ObjectMapper objectMapper = new ObjectMapper();
		PlatformLabelVerificationService service =
				new PlatformLabelVerificationService(orderService, objectMapper);

		ErpOrder order = new ErpOrder();
		order.setId(30L);
		order.setPlatformOrderId("OZON-ORDER-30");
		order.setShipmentId("OZON-SHIPMENT-30");
		order.setLabelVerifyCodes(objectMapper.writeValueAsString(
				Arrays.asList("OZON-REAL-BARCODE", "OZON-PACKAGE-BARCODE")));
		when(orderService.getById(30L)).thenReturn(order);

		WmsFulfillmentOrder fulfillment = new WmsFulfillmentOrder();
		fulfillment.setErpTenantId(6L);
		fulfillment.setSourceOrderId(30L);
		fulfillment.setSourceOrderNo("OZON-ORDER-30");

		assertThat(service.matches(fulfillment, "OZON-REAL-BARCODE")).isTrue();
		assertThat(service.matches(fulfillment, "OZON-SHIPMENT-30")).isTrue();
		assertThat(service.matches(fulfillment, "OZON-ORDER-30")).isTrue();
		assertThat(service.matches(fulfillment, "OTHER")).isFalse();
	}
}
