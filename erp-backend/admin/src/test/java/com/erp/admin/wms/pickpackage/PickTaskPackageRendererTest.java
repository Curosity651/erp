package com.erp.admin.wms.pickpackage;

import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskLine;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskOrder;
import com.erp.admin.wms.model.vo.FulfillmentPickTaskDetailVO;
import com.erp.admin.wms.model.vo.FulfillmentPickTaskOrderVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PickTaskPackageRendererTest {

	@Test
	void producesTwoCompletePackagesWithPlatformShopLayout() throws Exception {
		PickTaskPackageSnapshot snapshot = PickTaskPackageSnapshot.from(detail());
		Map<Long, byte[]> labels = new LinkedHashMap<>();
		labels.put(21L, PickTaskPackageRenderer.simplePdf(Collections.singletonList("LABEL ORDER-21")));
		PickTaskPackageFiles result = new PickTaskPackageRenderer(new ObjectMapper()).render(snapshot,
				labels, LocalDateTime.of(2026, 9, 15, 12, 30));

		assertThat(result.getWarehouseFileName()).isEqualTo("Sklad-FPT-20-20260915-1230.zip");
		assertThat(PickPackageArchive.read(result.getWarehouseZip()).keySet()).containsExactly(
				"00_Checklist_20260915-1230.pdf", "OZON/Pick_List_OZON.pdf",
				"OZON/SHOP-9/Labels_SHOP-9.pdf", "manifest.json");
		assertThat(PickPackageArchive.read(result.getArchiveZip()).keySet()).containsExactly(
				"00_汇总.xlsx", "manifest.json", "文件校验清单.json", "订单与SKU明细.xlsx", "跳过与异常.xlsx");
		assertThat(result.getWarehouseSha256()).hasSize(64);
		assertThat(result.getArchiveSha256()).hasSize(64);
		String qaOutput = System.getProperty("pickpackage.qa.output");
		if (qaOutput != null && !qaOutput.trim().isEmpty()) {
			Files.createDirectories(Paths.get(qaOutput));
			Map<String, byte[]> warehouse = PickPackageArchive.read(result.getWarehouseZip());
			Files.write(Paths.get(qaOutput, "checklist.pdf"),
					warehouse.get("00_Checklist_20260915-1230.pdf"));
			Files.write(Paths.get(qaOutput, "pick-list.pdf"),
					warehouse.get("OZON/Pick_List_OZON.pdf"));
			Map<String, byte[]> archive = PickPackageArchive.read(result.getArchiveZip());
			Files.write(Paths.get(qaOutput, "summary.xlsx"), archive.get("00_汇总.xlsx"));
			Files.write(Paths.get(qaOutput, "details.xlsx"), archive.get("订单与SKU明细.xlsx"));
			Files.write(Paths.get(qaOutput, "exceptions.xlsx"), archive.get("跳过与异常.xlsx"));
		}
	}

	private FulfillmentPickTaskDetailVO detail() {
		WmsFulfillmentPickTask task = new WmsFulfillmentPickTask();
		task.setId(20L); task.setTaskNo("FPT-20"); task.setWarehouseId(3L); task.setOperatorId(7L);
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(21L); order.setSourceType("OZON"); order.setShopId(9L); order.setSourceOrderNo("ORDER-21");
		WmsFulfillmentPickTaskOrder relation = new WmsFulfillmentPickTaskOrder();
		relation.setFulfillmentOrderId(21L); relation.setOrderStatus("PICKING");
		WmsFulfillmentPickTaskLine line = new WmsFulfillmentPickTaskLine();
		line.setLocationCode("A-01"); line.setWarehouseSkuCode("WH-1"); line.setSkuCode("SKU-1"); line.setPlannedQuantity(2);
		FulfillmentPickTaskOrderVO row = new FulfillmentPickTaskOrderVO();
		row.setTaskOrder(relation); row.setFulfillmentOrder(order); row.setRouteLines(Collections.singletonList(line));
		FulfillmentPickTaskDetailVO detail = new FulfillmentPickTaskDetailVO();
		detail.setTask(task); detail.setOrderQueue(Collections.singletonList(row));
		return detail;
	}
}
