package com.erp.admin.wms;

import java.util.Arrays;
import java.util.Collections;

import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.service.WmsInventoryAggregator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 库存聚合纯逻辑测试（D1·方案②）：三桶折算。
 *
 * @author erp
 */
class WmsInventoryAggregatorTest {

	private WmsPhysicalInventory batch(String quality, int allocatable, int qty, int reserved) {
		WmsPhysicalInventory b = new WmsPhysicalInventory();
		b.setQuality(quality);
		b.setAllocatable(allocatable);
		b.setQuantity(qty);
		b.setReservedQty(reserved);
		return b;
	}

	@Test
	void empty_batches_all_zero() {
		WmsInventoryAggregator.Buckets b = WmsInventoryAggregator.aggregate(Collections.emptyList());
		assertThat(b.available).isZero();
		assertThat(b.reserved).isZero();
		assertThat(b.damaged).isZero();
	}

	@Test
	void good_allocatable_sums_available_minus_reserved() {
		// 100良品(锁10) + 50良品(锁0) → available=(100-10)+(50-0)=140, reserved=10
		WmsInventoryAggregator.Buckets b = WmsInventoryAggregator
			.aggregate(Arrays.asList(batch("GOOD", 1, 100, 10), batch("GOOD", 1, 50, 0)));
		assertThat(b.available).isEqualTo(140);
		assertThat(b.reserved).isEqualTo(10);
		assertThat(b.damaged).isZero();
	}

	@Test
	void damaged_goes_to_damaged_bucket_only() {
		// 30次品 → damaged=30，不计入 available
		WmsInventoryAggregator.Buckets b = WmsInventoryAggregator
			.aggregate(Arrays.asList(batch("GOOD", 1, 100, 0), batch("DAMAGED", 0, 30, 0)));
		assertThat(b.available).isEqualTo(100);
		assertThat(b.damaged).isEqualTo(30);
		assertThat(b.reserved).isZero();
	}

	@Test
	void good_but_not_allocatable_excluded_from_available() {
		// 良品但不可分配(暂存区) → 不计 available，但 reserved 仍统计
		WmsInventoryAggregator.Buckets b = WmsInventoryAggregator
			.aggregate(Arrays.asList(batch("GOOD", 0, 40, 5), batch("GOOD", 1, 60, 0)));
		assertThat(b.available).isEqualTo(60);
		assertThat(b.reserved).isEqualTo(5);
		assertThat(b.damaged).isZero();
	}

	@Test
	void null_quantities_treated_as_zero() {
		WmsInventoryAggregator.Buckets b = WmsInventoryAggregator
			.aggregate(Collections.singletonList(batch("GOOD", 1, 0, 0)));
		assertThat(b.available).isZero();
	}

}
