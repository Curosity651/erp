package com.erp.admin.wms;

import com.erp.admin.wms.model.dto.StockPostingItemDTO;
import com.erp.admin.wms.model.entity.StockPostingItem;
import com.erp.admin.wms.model.enums.StockBucket;
import com.erp.admin.wms.model.enums.StockDirection;
import com.erp.admin.wms.service.InventoryPostingEngine;
import com.erp.admin.wms.service.StockPostingService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 过账「按货主隔离」纯逻辑测试（P5/P7）：
 * 不同货主(erp_tenant_id)的同名 SKU 在同一仓库/区域必须分属不同的合并键 / 分组键，
 * 绝不能被合并到同一条库存记录或同一条过账明细。
 *
 * @author erp
 */
class StockPostingTenantIsolationTest {

	private StockPostingItemDTO regionItemDto(Long erpTenantId, Long regionId, String sku,
			StockBucket bucket, StockDirection direction, int qty) {
		return StockPostingItemDTO.builder()
				.warehouseId(0L)
				.regionId(regionId)
				.erpTenantId(erpTenantId)
				.skuCode(sku)
				.bucket(bucket)
				.direction(direction)
				.quantity(qty)
				.build();
	}

	private StockPostingItem regionItem(Long erpTenantId, Long regionId, String sku) {
		StockPostingItem item = new StockPostingItem();
		item.setWarehouseId(0L);
		item.setRegionId(regionId);
		item.setErpTenantId(erpTenantId);
		item.setSkuCode(sku);
		return item;
	}

	@Test
	void merge_key_separates_different_owners_for_same_sku() {
		StockPostingItemDTO ownerA = regionItemDto(100L, 9L, "SKU-1",
				StockBucket.RESERVED, StockDirection.OUT, 3);
		StockPostingItemDTO ownerB = regionItemDto(200L, 9L, "SKU-1",
				StockBucket.RESERVED, StockDirection.OUT, 5);

		assertThat(StockPostingService.buildMergeKey(ownerA))
				.isNotEqualTo(StockPostingService.buildMergeKey(ownerB));
	}

	@Test
	void merge_key_equal_for_same_owner_and_dimensions() {
		StockPostingItemDTO a = regionItemDto(100L, 9L, "SKU-1",
				StockBucket.RESERVED, StockDirection.OUT, 3);
		StockPostingItemDTO b = regionItemDto(100L, 9L, "SKU-1",
				StockBucket.RESERVED, StockDirection.OUT, 2);

		assertThat(StockPostingService.buildMergeKey(a))
				.isEqualTo(StockPostingService.buildMergeKey(b));
	}

	@Test
	void region_group_key_separates_different_owners() {
		assertThat(InventoryPostingEngine.regionGroupKey(regionItem(100L, 9L, "SKU-1")))
				.isNotEqualTo(InventoryPostingEngine.regionGroupKey(regionItem(200L, 9L, "SKU-1")));
	}

	@Test
	void region_group_key_equal_for_same_owner_region_sku() {
		assertThat(InventoryPostingEngine.regionGroupKey(regionItem(100L, 9L, "SKU-1")))
				.isEqualTo(InventoryPostingEngine.regionGroupKey(regionItem(100L, 9L, "SKU-1")));
	}

	private StockPostingItem warehouseItem(Long erpTenantId, Long warehouseId, String sku) {
		StockPostingItem item = new StockPostingItem();
		item.setRegionId(0L);
		item.setWarehouseId(warehouseId);
		item.setErpTenantId(erpTenantId);
		item.setSkuCode(sku);
		return item;
	}

	@Test
	void warehouse_group_key_separates_different_owners() {
		assertThat(InventoryPostingEngine.warehouseGroupKey(warehouseItem(100L, 7L, "SKU-1")))
				.isNotEqualTo(InventoryPostingEngine.warehouseGroupKey(warehouseItem(200L, 7L, "SKU-1")));
	}

	@Test
	void warehouse_group_key_equal_for_same_owner_warehouse_sku() {
		assertThat(InventoryPostingEngine.warehouseGroupKey(warehouseItem(100L, 7L, "SKU-1")))
				.isEqualTo(InventoryPostingEngine.warehouseGroupKey(warehouseItem(100L, 7L, "SKU-1")));
	}
}
