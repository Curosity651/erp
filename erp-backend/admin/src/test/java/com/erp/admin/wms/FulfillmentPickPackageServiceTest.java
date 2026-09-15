package com.erp.admin.wms;

import java.util.Collections;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.erp.admin.system.service.OssService;
import com.erp.admin.wms.mapper.WmsFulfillmentPickPackageMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskLine;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskOrder;
import com.erp.admin.wms.model.vo.FulfillmentPickPackageVO;
import com.erp.admin.wms.model.vo.FulfillmentPickTaskDetailVO;
import com.erp.admin.wms.model.vo.FulfillmentPickTaskOrderVO;
import com.erp.admin.wms.pickpackage.PickTaskPackageRenderer;
import com.erp.admin.wms.service.FulfillmentPickPackageService;
import com.erp.admin.wms.service.FulfillmentPickingService;
import com.erp.admin.wms.service.FulfillmentShippingService;
import com.erp.admin.wms.service.platform.PlatformLabelResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FulfillmentPickPackageServiceTest {

	@Test
	void manualTaskBuildsBothPrivateFilesWithoutShippingOrBillingActions() {
		FulfillmentPickingService picking = mock(FulfillmentPickingService.class);
		FulfillmentShippingService shipping = mock(FulfillmentShippingService.class);
		WmsFulfillmentPickPackageMapper mapper = mock(WmsFulfillmentPickPackageMapper.class);
		OssService oss = mock(OssService.class);
		when(picking.detail(1L)).thenReturn(detail());
		when(mapper.selectOne(any(Wrapper.class))).thenReturn(null);
		when(shipping.printLabel(2L, 7L)).thenReturn(
				PlatformLabelResult.success("MANUAL-2", "/manual-label", "MANUAL-2"));
		when(oss.putObject(anyString(), any(byte[].class), anyString()))
				.thenAnswer(invocation -> invocation.getArgument(2));
		when(oss.getDownloadUrl(anyString(), anyString()))
				.thenAnswer(invocation -> "https://download/" + invocation.getArgument(1));
		FulfillmentPickPackageService service = new FulfillmentPickPackageService(picking, shipping,
				mapper, new PickTaskPackageRenderer(new ObjectMapper()), oss);

		FulfillmentPickPackageVO result = service.generate(1L, 7L);

		assertThat(result.getWarehouseFileName()).startsWith("Sklad-FPT-1-").endsWith(".zip");
		assertThat(result.getArchiveFileName()).startsWith("拣货任务-FPT-1-").endsWith("-中文.zip");
		assertThat(result.getOrderCount()).isEqualTo(1);
		assertThat(result.getTotalQuantity()).isEqualTo(2);
		verify(shipping).printLabel(2L, 7L);
		verify(oss, org.mockito.Mockito.times(2)).putObject(anyString(), any(byte[].class), anyString());
	}

	private FulfillmentPickTaskDetailVO detail() {
		WmsFulfillmentPickTask task = new WmsFulfillmentPickTask();
		task.setId(1L); task.setTaskNo("FPT-1"); task.setWarehouseId(3L); task.setOperatorId(7L);
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(2L); order.setSourceType("MANUAL"); order.setShopId(4L); order.setSourceOrderNo("MANUAL-2");
		WmsFulfillmentPickTaskOrder relation = new WmsFulfillmentPickTaskOrder();
		relation.setFulfillmentOrderId(2L); relation.setOrderStatus("PICKING");
		WmsFulfillmentPickTaskLine line = new WmsFulfillmentPickTaskLine();
		line.setLocationCode("A-01"); line.setWarehouseSkuCode("WH-1"); line.setSkuCode("SKU-1"); line.setPlannedQuantity(2);
		FulfillmentPickTaskOrderVO row = new FulfillmentPickTaskOrderVO();
		row.setTaskOrder(relation); row.setFulfillmentOrder(order); row.setRouteLines(Collections.singletonList(line));
		FulfillmentPickTaskDetailVO detail = new FulfillmentPickTaskDetailVO();
		detail.setTask(task); detail.setOrderQueue(Collections.singletonList(row));
		return detail;
	}
}
