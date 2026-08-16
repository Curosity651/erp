package com.erp.admin.wms.platform;

import java.util.Collections;

import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPlatformActionMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentPlatformAction;
import com.erp.admin.wms.service.FulfillmentPlatformActionService;
import com.erp.admin.wms.service.platform.FulfillmentPlatformAdapter;
import com.erp.admin.wms.service.platform.PlatformActionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FulfillmentPlatformActionServiceTest {
	@Test
	void executes_platform_action_once_and_persists_success() {
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		WmsFulfillmentPlatformActionMapper actionMapper = mock(WmsFulfillmentPlatformActionMapper.class);
		FulfillmentPlatformAdapter adapter = mock(FulfillmentPlatformAdapter.class);
		when(adapter.sourceType()).thenReturn("OZON");
		when(adapter.accept(any())).thenReturn(PlatformActionResult.success("accepted", "EXT-1"));
		when(orderMapper.selectById(10L)).thenReturn(order());
		when(actionMapper.selectForUpdate(10L, "ACCEPT")).thenReturn(null);
		when(actionMapper.insert(any())).thenReturn(1);
		when(actionMapper.updateById(any())).thenReturn(1);
		FulfillmentPlatformActionService service = new FulfillmentPlatformActionService(orderMapper,
				actionMapper, Collections.singletonList(adapter), new ObjectMapper());

		PlatformActionResult result = service.accept(10L);

		assertThat(result.isSuccess()).isTrue();
		verify(adapter).accept(any());
		verify(actionMapper).insert(any(WmsFulfillmentPlatformAction.class));
		verify(actionMapper).updateById(any(WmsFulfillmentPlatformAction.class));
	}

	@Test
	void repeated_success_returns_saved_result_without_calling_platform() {
		WmsFulfillmentOrderMapper orderMapper = mock(WmsFulfillmentOrderMapper.class);
		WmsFulfillmentPlatformActionMapper actionMapper = mock(WmsFulfillmentPlatformActionMapper.class);
		FulfillmentPlatformAdapter adapter = mock(FulfillmentPlatformAdapter.class);
		when(adapter.sourceType()).thenReturn("OZON");
		when(orderMapper.selectById(10L)).thenReturn(order());
		WmsFulfillmentPlatformAction existing = new WmsFulfillmentPlatformAction();
		existing.setActionStatus("SUCCEEDED");
		existing.setResponsePayload("{\"success\":true,\"message\":\"accepted\",\"externalReference\":\"EXT-1\"}");
		when(actionMapper.selectForUpdate(10L, "ACCEPT")).thenReturn(existing);
		FulfillmentPlatformActionService service = new FulfillmentPlatformActionService(orderMapper,
				actionMapper, Collections.singletonList(adapter), new ObjectMapper());

		PlatformActionResult result = service.accept(10L);

		assertThat(result.getExternalReference()).isEqualTo("EXT-1");
		verify(adapter, never()).accept(any());
	}

	private WmsFulfillmentOrder order() {
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(10L);
		order.setSourceType("OZON");
		order.setSourceOrderId(20L);
		order.setVersion(0);
		return order;
	}
}
