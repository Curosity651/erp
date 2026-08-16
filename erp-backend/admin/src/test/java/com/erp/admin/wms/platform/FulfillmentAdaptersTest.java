package com.erp.admin.wms.platform;

import java.util.Collections;

import com.erp.admin.order.model.vo.LabelBatchFileVO;
import com.erp.admin.order.model.vo.LabelBatchVO;
import com.erp.admin.order.service.common.model.ConfirmResult;
import com.erp.admin.order.service.label.LabelPrintOrchestrator;
import com.erp.admin.order.service.ozon.OzonOrderConfirmService;
import com.erp.admin.order.service.wildberries.WbOrderConfirmService;
import com.erp.admin.order.service.yandex.YdOrderConfirmService;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.service.platform.ManualFulfillmentAdapter;
import com.erp.admin.wms.service.platform.OzonFulfillmentAdapter;
import com.erp.admin.wms.service.platform.WbFulfillmentAdapter;
import com.erp.admin.wms.service.platform.YandexFulfillmentAdapter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FulfillmentAdaptersTest {
	@Test
	void ozon_accepts_before_fetching_platform_label() {
		OzonOrderConfirmService confirmService = mock(OzonOrderConfirmService.class);
		LabelPrintOrchestrator labels = mock(LabelPrintOrchestrator.class);
		when(confirmService.confirmOrders(anyList())).thenReturn(successConfirm(20L));
		when(labels.printLabels(anyString(), anyList(), anyLong(), anyString())).thenReturn(labelBatch());
		OzonFulfillmentAdapter adapter = new OzonFulfillmentAdapter(confirmService, labels);

		assertThat(adapter.accept(order("OZON")).isSuccess()).isTrue();
		assertThat(adapter.fetchLabel(order("OZON")).getLabelUrl()).isEqualTo("/label.pdf");
	}

	@Test
	void wb_prepares_supply_on_accept_and_ships_only_on_finalize() {
		WbOrderConfirmService confirmService = mock(WbOrderConfirmService.class);
		LabelPrintOrchestrator labels = mock(LabelPrintOrchestrator.class);
		when(confirmService.prepareOrders(anyList())).thenReturn(successConfirm(20L));
		when(confirmService.finalizeOrders(anyList())).thenReturn(successConfirm(20L));
		WbFulfillmentAdapter adapter = new WbFulfillmentAdapter(confirmService, labels);

		assertThat(adapter.accept(order("WB")).isSuccess()).isTrue();
		verify(confirmService, never()).finalizeOrders(anyList());
		assertThat(adapter.finalizeShipment(order("WB")).isSuccess()).isTrue();
		verify(confirmService).finalizeOrders(Collections.singletonList(20L));
	}

	@Test
	void yandex_accept_is_local_and_platform_confirmation_waits_for_mark_ready() {
		YdOrderConfirmService confirmService = mock(YdOrderConfirmService.class);
		LabelPrintOrchestrator labels = mock(LabelPrintOrchestrator.class);
		when(confirmService.confirmOrders(anyList())).thenReturn(successConfirm(20L));
		YandexFulfillmentAdapter adapter = new YandexFulfillmentAdapter(confirmService, labels);

		assertThat(adapter.accept(order("YANDEX")).isSuccess()).isTrue();
		verify(confirmService, never()).confirmOrders(anyList());
		assertThat(adapter.markReady(order("YANDEX")).isSuccess()).isTrue();
		verify(confirmService).confirmOrders(Collections.singletonList(20L));
	}

	@Test
	void manual_order_uses_local_system_label() {
		ManualFulfillmentAdapter adapter = new ManualFulfillmentAdapter();
		assertThat(adapter.accept(order("MANUAL")).isSuccess()).isTrue();
		assertThat(adapter.fetchLabel(order("MANUAL")).getLabelUrl())
				.isEqualTo("/wms/fulfillment/10/manual-label");
	}

	private WmsFulfillmentOrder order(String sourceType) {
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(10L);
		order.setSourceType(sourceType);
		order.setSourceOrderId(20L);
		order.setSourceOrderNo("ORDER-20");
		return order;
	}

	private ConfirmResult successConfirm(Long orderId) {
		ConfirmResult.Item item = new ConfirmResult.Item();
		item.setOrderId(orderId);
		item.setSuccess(true);
		item.setMessage("OK");
		ConfirmResult result = new ConfirmResult();
		result.setItems(Collections.singletonList(item));
		return result;
	}

	private LabelBatchVO labelBatch() {
		LabelBatchFileVO file = new LabelBatchFileVO();
		file.setDownloadUrl("/label.pdf");
		LabelBatchVO batch = new LabelBatchVO();
		batch.setBatchId(30L);
		batch.setSuccessCount(1);
		batch.setFailedCount(0);
		batch.setFiles(Collections.singletonList(file));
		return batch;
	}
}
