package com.erp.admin.wms;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.erp.admin.wms.model.dto.InboundPutawayDTO;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.service.LogicalInboundPutawayService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LogicalInboundPutawayServiceTest {

	@Test
	void split_across_locations_keeps_exact_received_total() {
		Map<String, Integer> received = new LinkedHashMap<>();
		received.put("SKU-A", 10);

		List<InboundPutawayDTO.PutawayLine> lines = Arrays.asList(
				line(1L, "SKU-A", 6, "GOOD"), line(2L, "SKU-A", 4, "GOOD"));

		LogicalInboundPutawayService.validateAllocationTotals(received, lines);
	}

	@Test
	void duplicate_location_sku_quality_is_merged_before_receipt_write() {
		List<InboundPutawayDTO.PutawayLine> merged = LogicalInboundPutawayService.mergeAllocations(Arrays.asList(
				line(1L, "SKU-A", 6, null), line(1L, "SKU-A", 4, "GOOD"),
				line(2L, "SKU-A", 2, "GOOD")));

		assertThat(merged).hasSize(2);
		assertThat(merged.get(0).getQuantity()).isEqualTo(10);
		assertThat(merged.get(0).getQuality()).isEqualTo("GOOD");
	}

	@Test
	void mismatched_total_is_rejected() {
		Map<String, Integer> received = new LinkedHashMap<>();
		received.put("SKU-A", 10);

		assertThatIllegalArgumentException().isThrownBy(() -> LogicalInboundPutawayService
				.validateAllocationTotals(received, Arrays.asList(line(1L, "SKU-A", 9, "GOOD"))));
	}

	@Test
	void defective_and_good_goods_cannot_cross_zone_types() {
		WmsLocation location = new WmsLocation();
		location.setLocationType("BIG");

		LogicalInboundPutawayService.validateTargetType("GOOD", location, "STANDARD");
		LogicalInboundPutawayService.validateTargetType("DAMAGED", location, "DEFECTIVE");
		assertThatIllegalArgumentException().isThrownBy(
				() -> LogicalInboundPutawayService.validateTargetType("DAMAGED", location, "STANDARD"));
		assertThatIllegalArgumentException().isThrownBy(
				() -> LogicalInboundPutawayService.validateTargetType("GOOD", location, "DEFECTIVE"));
	}

	private InboundPutawayDTO.PutawayLine line(Long locationId, String sku, int quantity, String quality) {
		InboundPutawayDTO.PutawayLine line = new InboundPutawayDTO.PutawayLine();
		line.setLocationId(locationId);
		line.setSkuCode(sku);
		line.setQuantity(quantity);
		line.setQuality(quality);
		return line;
	}

}
