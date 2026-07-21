package com.erp.admin.wms.service;

import com.erp.admin.wms.model.dto.StockPostingDTO;
import com.erp.admin.wms.model.dto.StockPostingItemDTO;
import com.erp.admin.wms.model.enums.PostingType;
import com.erp.admin.wms.model.enums.StockBucket;
import com.erp.admin.wms.model.enums.StockDirection;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockPostingTenantResolutionTest {

	@Test
	void single_owner_is_copied_to_posting_header() {
		StockPostingDTO dto = posting(null);

		boolean aggregate = StockPostingService.resolvePostingTenant(dto,
				Collections.singletonList(item(6L, "SKU-1")));

		assertThat(aggregate).isFalse();
		assertThat(dto.getErpTenantId()).isEqualTo(6L);
	}

	@Test
	void multiple_owners_create_aggregate_posting_with_null_header_owner() {
		StockPostingDTO dto = posting(null);

		boolean aggregate = StockPostingService.resolvePostingTenant(dto,
				Arrays.asList(item(6L, "SKU-1"), item(7L, "SKU-2")));

		assertThat(aggregate).isTrue();
		assertThat(dto.getErpTenantId()).isNull();
	}

	@Test
	void missing_or_non_positive_item_owner_is_rejected() {
		assertThatThrownBy(() -> StockPostingService.resolvePostingTenant(posting(null),
				Collections.singletonList(item(null, "SKU-1"))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("有效货主");
		assertThatThrownBy(() -> StockPostingService.resolvePostingTenant(posting(null),
				Collections.singletonList(item(0L, "SKU-1"))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("有效货主");
	}

	@Test
	void header_owner_must_match_all_items() {
		assertThatThrownBy(() -> StockPostingService.resolvePostingTenant(posting(6L),
				Collections.singletonList(item(7L, "SKU-1"))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("不一致");
	}

	private StockPostingDTO posting(Long ownerId) {
		return StockPostingDTO.builder()
				.erpTenantId(ownerId)
				.postingType(PostingType.STOCKTAKE)
				.build();
	}

	private StockPostingItemDTO item(Long ownerId, String skuCode) {
		return StockPostingItemDTO.builder()
				.warehouseId(1L)
				.regionId(0L)
				.erpTenantId(ownerId)
				.skuCode(skuCode)
				.bucket(StockBucket.AVAILABLE)
				.direction(StockDirection.IN)
				.quantity(1)
				.build();
	}
}
