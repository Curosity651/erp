package com.erp.admin.wms.pickpackage;

import java.util.Arrays;
import java.util.Collections;

import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskLine;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskOrder;
import com.erp.admin.wms.model.vo.FulfillmentPickTaskDetailVO;
import com.erp.admin.wms.model.vo.FulfillmentPickTaskOrderVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PickTaskPackageSnapshotTest {

	@Test
	void createsStableSortedSnapshotAndSeparatesCancelledOrders() {
		FulfillmentPickTaskOrderVO active = row(3L, "WB", 8L, "ORDER-2", "ACTIVE",
				line("B-02", "WH-2", "SKU-2", 2), line("A-01", "WH-1", "SKU-1", 1));
		FulfillmentPickTaskOrderVO cancelled = row(2L, "OZON", 9L, "ORDER-1", "CANCELLED",
				line("A-01", "WH-X", "SKU-X", 5));
		PickTaskPackageSnapshot first = PickTaskPackageSnapshot.from(detail(active, cancelled));
		PickTaskPackageSnapshot second = PickTaskPackageSnapshot.from(detail(cancelled, active));

		assertThat(first.getOrders()).hasSize(1);
		assertThat(first.getExcludedOrders()).hasSize(1);
		assertThat(first.getTotalQuantity()).isEqualTo(3);
		assertThat(first.getOrders().get(0).getLines()).extracting(PickTaskPackageSnapshot.Line::getLocationCode)
				.containsExactly("A-01", "B-02");
		assertThat(first.getSnapshotHash()).isEqualTo(second.getSnapshotHash()).hasSize(64);
	}

	@Test
	void rejectsUnknownPlatformAndEmptyLines() {
		assertThatIllegalArgumentException().isThrownBy(() -> PickTaskPackageSnapshot.from(
				detail(row(1L, "UNKNOWN", 1L, "X", "ACTIVE", line("A", "B", "C", 1)))));
		FulfillmentPickTaskOrderVO row = row(1L, "OZON", 1L, "X", "ACTIVE",
				line("A", "B", "C", 1));
		row.setRouteLines(Collections.emptyList());
		assertThatIllegalArgumentException().isThrownBy(() -> PickTaskPackageSnapshot.from(detail(row)));
	}

	private FulfillmentPickTaskDetailVO detail(FulfillmentPickTaskOrderVO... rows) {
		WmsFulfillmentPickTask task = new WmsFulfillmentPickTask();
		task.setId(11L); task.setTaskNo("FPT-11"); task.setWarehouseId(3L); task.setOperatorId(7L);
		FulfillmentPickTaskDetailVO detail = new FulfillmentPickTaskDetailVO();
		detail.setTask(task); detail.setOrderQueue(Arrays.asList(rows));
		return detail;
	}

	private FulfillmentPickTaskOrderVO row(Long id, String platform, Long shopId, String orderNo,
			String status, WmsFulfillmentPickTaskLine... lines) {
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(id); order.setSourceType(platform); order.setShopId(shopId); order.setSourceOrderNo(orderNo);
		WmsFulfillmentPickTaskOrder taskOrder = new WmsFulfillmentPickTaskOrder();
		taskOrder.setFulfillmentOrderId(id); taskOrder.setOrderStatus(status);
		FulfillmentPickTaskOrderVO row = new FulfillmentPickTaskOrderVO();
		row.setFulfillmentOrder(order); row.setTaskOrder(taskOrder); row.setRouteLines(Arrays.asList(lines));
		return row;
	}

	private WmsFulfillmentPickTaskLine line(String location, String warehouseSku, String sku, int qty) {
		WmsFulfillmentPickTaskLine line = new WmsFulfillmentPickTaskLine();
		line.setLocationCode(location); line.setWarehouseSkuCode(warehouseSku);
		line.setSkuCode(sku); line.setPlannedQuantity(qty);
		return line;
	}
}
