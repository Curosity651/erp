package com.erp.admin.order;

import java.util.Collections;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.order.mapper.ErpOrderItemMapper;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.dto.SubmitFulfillmentDTO;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.order.service.ErpOrderFulfillmentSubmissionService;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.shop.mapper.ShopMapper;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.wms.model.dto.FulfillmentCreateCommand;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.service.FulfillmentOrderService;
import com.erp.admin.wms.service.WarehouseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpOrderFulfillmentSubmissionTest {
	@AfterEach
	void clearTenant() {
		TenantContext.clear();
		WmsTenantContext.clear();
	}

	@Test
	void uses_shop_default_internal_warehouse_and_builds_owner_sku_snapshot() {
		ErpOrderMapper orderMapper = mock(ErpOrderMapper.class);
		ErpOrderItemMapper itemMapper = mock(ErpOrderItemMapper.class);
		ShopMapper shopMapper = mock(ShopMapper.class);
		SkuMapper skuMapper = mock(SkuMapper.class);
		WarehouseSkuCodeService warehouseSkuCodeService = mock(WarehouseSkuCodeService.class);
		WarehouseService warehouseService = mock(WarehouseService.class);
		FulfillmentOrderService fulfillmentOrderService = mock(FulfillmentOrderService.class);
		ErpOrderFulfillmentSubmissionService service = new ErpOrderFulfillmentSubmissionService(orderMapper,
				itemMapper, shopMapper, skuMapper, warehouseSkuCodeService, warehouseService,
				fulfillmentOrderService);

		ErpOrder order = new ErpOrder();
		order.setId(10L);
		order.setShopId(20L);
		order.setPlatform("ozon");
		order.setPlatformOrderId("OZ-1");
		order.setFulfillmentType("FBS");
		Shop shop = new Shop();
		shop.setId(20L);
		shop.setDefaultWmsWarehouseId(30L);
		ErpOrderItem orderItem = new ErpOrderItem();
		orderItem.setSkuCode("SKU-A");
		orderItem.setQuantity(2);
		Sku sku = new Sku();
		sku.setSkuCode("SKU-A");
		sku.setOuterLengthMm(100);
		sku.setOuterWidthMm(200);
		sku.setOuterHeightMm(300);
		sku.setOuterGrossWeightG(4000);
		when(orderMapper.selectById(10L)).thenReturn(order);
		when(shopMapper.selectById(20L)).thenReturn(shop);
		when(warehouseService.validateOperableOwnWarehouse(30L)).thenReturn(new Warehouse());
		when(itemMapper.selectByOrderId(10L)).thenReturn(Collections.singletonList(orderItem));
		when(skuMapper.selectBySkuCode("SKU-A")).thenReturn(sku);
		when(warehouseSkuCodeService.build(3L, "SKU-A")).thenReturn("JHIN-SKU-A");
		when(fulfillmentOrderService.createAndReserve(any())).thenReturn(99L);
		when(orderMapper.updateById(any())).thenReturn(1);
		TenantContext.setCurrentTenant(3L);
		WmsTenantContext.setCurrentWmsTenant(4L);
		SubmitFulfillmentDTO dto = new SubmitFulfillmentDTO();
		dto.setErpOrderId(10L);

		assertThat(service.submit(dto)).isEqualTo(99L);
		ArgumentCaptor<FulfillmentCreateCommand> captor = ArgumentCaptor.forClass(FulfillmentCreateCommand.class);
		verify(fulfillmentOrderService).createAndReserve(captor.capture());
		assertThat(captor.getValue().getWarehouseId()).isEqualTo(30L);
		assertThat(captor.getValue().getSourceType()).isEqualTo("OZON");
		assertThat(captor.getValue().getItems().get(0).getWarehouseSkuCode()).isEqualTo("JHIN-SKU-A");
	}
}
