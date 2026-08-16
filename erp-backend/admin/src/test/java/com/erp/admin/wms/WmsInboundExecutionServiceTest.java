package com.erp.admin.wms;

import com.erp.admin.wms.model.dto.InboundPutawayDTO;
import com.erp.admin.wms.model.dto.InboundReceiveDTO;
import com.erp.admin.wms.model.entity.PurchaseInboundOrderItem;
import com.erp.admin.wms.service.WmsInboundExecutionService;
import com.erp.admin.wms.service.WmsInboundExecutionService.MergedLine;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 平台收货/上架 纯逻辑测试（D2.4）：上架行合并、按SKU汇总、覆盖校验、品质归一。
 *
 * @author erp
 */
class WmsInboundExecutionServiceTest {

	private InboundPutawayDTO.PutawayLine line(String sku, String loc, int qty, String quality) {
		InboundPutawayDTO.PutawayLine l = new InboundPutawayDTO.PutawayLine();
		l.setSkuCode(sku);
		l.setLocationCode(loc);
		l.setQuantity(qty);
		l.setQuality(quality);
		return l;
	}

	@Test
	void merge_same_sku_location_quality_accumulates() {
		List<MergedLine> merged = WmsInboundExecutionService.mergeLines(
				Arrays.asList(line("A", "L1", 3, "GOOD"), line("A", "L1", 2, "GOOD")));
		assertThat(merged).hasSize(1);
		assertThat(merged.get(0).quantity).isEqualTo(5);
	}

	@Test
	void merge_keeps_distinct_locations_and_quality() {
		List<MergedLine> merged = WmsInboundExecutionService.mergeLines(
				Arrays.asList(line("A", "L1", 3, "GOOD"), line("A", "L2", 4, "GOOD"), line("A", "L1", 1, "DAMAGED")));
		assertThat(merged).hasSize(3);
	}

	@Test
	void sum_by_sku_totals_across_locations() {
		List<MergedLine> merged = WmsInboundExecutionService.mergeLines(
				Arrays.asList(line("A", "L1", 3, "GOOD"), line("A", "L2", 4, "GOOD"), line("B", "L3", 5, "GOOD")));
		Map<String, Integer> bySku = WmsInboundExecutionService.sumBySku(merged);
		assertThat(bySku).containsEntry("A", 7).containsEntry("B", 5);
	}

	@Test
	void covers_exactly_true_when_totals_match() {
		Map<String, Integer> received = new HashMap<>();
		received.put("A", 7);
		received.put("B", 5);
		Map<String, Integer> putaway = new HashMap<>();
		putaway.put("A", 7);
		putaway.put("B", 5);
		assertThat(WmsInboundExecutionService.coversExactly(received, putaway)).isTrue();
	}

	@Test
	void covers_exactly_false_on_quantity_or_sku_mismatch() {
		Map<String, Integer> received = new HashMap<>();
		received.put("A", 7);
		Map<String, Integer> less = new HashMap<>();
		less.put("A", 6);
		assertThat(WmsInboundExecutionService.coversExactly(received, less)).isFalse();

		Map<String, Integer> extra = new HashMap<>();
		extra.put("A", 7);
		extra.put("B", 1);
		assertThat(WmsInboundExecutionService.coversExactly(received, extra)).isFalse();
	}

	@Test
	void normalize_quality_defaults_to_good() {
		List<MergedLine> merged = WmsInboundExecutionService.mergeLines(Arrays.asList(line("A", "L1", 1, null)));
		assertThat(merged.get(0).quality).isEqualTo("GOOD");
	}

	@Test
	void receive_keeps_duplicate_sku_lines_separate_by_item_id() {
		PurchaseInboundOrderItem first = new PurchaseInboundOrderItem();
		first.setId(101L);
		first.setSkuCode("SKU-A");
		PurchaseInboundOrderItem second = new PurchaseInboundOrderItem();
		second.setId(102L);
		second.setSkuCode("SKU-A");

		InboundReceiveDTO.ReceiveItem firstReceived = new InboundReceiveDTO.ReceiveItem();
		firstReceived.setInboundOrderItemId(101L);
		firstReceived.setSkuCode("SKU-A");
		firstReceived.setActualQuantity(5);
		InboundReceiveDTO.ReceiveItem secondReceived = new InboundReceiveDTO.ReceiveItem();
		secondReceived.setInboundOrderItemId(102L);
		secondReceived.setSkuCode("SKU-A");
		secondReceived.setActualQuantity(7);

		Map<Long, Integer> quantities = WmsInboundExecutionService.mapReceivedByItemId(
				Arrays.asList(first, second), Arrays.asList(firstReceived, secondReceived));

		assertThat(quantities).containsEntry(101L, 5).containsEntry(102L, 7).hasSize(2);
	}

	@Test
	void receiving_evidence_ids_are_stored_once_in_scan_order() {
		assertThat(WmsInboundExecutionService.joinEvidenceFileIds(Arrays.asList(12L, 9L, 12L)))
				.isEqualTo("12,9");
	}

}
