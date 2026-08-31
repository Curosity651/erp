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
import com.erp.admin.order.service.FulfillmentRecipientSnapshotService;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.shop.mapper.ShopMapper;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.wms.model.dto.FulfillmentCreateCommand;
import com.erp.admin.wms.model.entity.WmsLogisticsProduct;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import com.erp.admin.wms.service.FulfillmentOrderService;
import com.erp.admin.wms.service.WarehouseService;
import com.erp.admin.wms.service.WmsLogisticsProductService;

import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
		WmsLogisticsProductService logisticsProductService = mock(WmsLogisticsProductService.class);
		ErpOrderFulfillmentSubmissionService service = new ErpOrderFulfillmentSubmissionService(orderMapper,
				itemMapper, shopMapper, skuMapper, warehouseSkuCodeService, warehouseService,
				fulfillmentOrderService, logisticsProductService,
				new FulfillmentRecipientSnapshotService(new com.fasterxml.jackson.databind.ObjectMapper()));

		ErpOrder order = new ErpOrder();
		order.setId(10L);
		order.setShopId(20L);
		order.setPlatform("ozon");
		order.setPlatformOrderId("OZ-1");
		order.setFulfillmentType("FBS");
		Shop shop = new Shop();
		shop.setId(20L);
		shop.setDefaultWmsWarehouseId(30L);
		shop.setDefaultLogisticsProductId(7L);
		WmsLogisticsProduct product = new WmsLogisticsProduct();
		product.setId(8L);
		product.setProductCode("EXPRESS");
		product.setProductName("加急派送");
		product.setProductDescription("仓库根据实际包裹选择承运方式");
		product.setUnitPrice(new BigDecimal("48.00"));
		product.setCurrency("RUB");
		ErpOrderItem orderItem = new ErpOrderItem();
		orderItem.setSkuCode("SKU-A");
		orderItem.setQuantity(2);
		Sku sku = new Sku();
		sku.setSkuCode("SKU-A");
		sku.setOuterLengthMm(100);
		sku.setOuterWidthMm(200);
		sku.setOuterHeightMm(300);
		sku.setOuterGrossWeightG(4000);
		when(orderMapper.selectForFulfillmentSubmit(10L, 3L)).thenReturn(order);
		when(shopMapper.selectById(20L)).thenReturn(shop);
		when(warehouseService.validateOperableOwnWarehouse(30L)).thenReturn(new Warehouse());
		when(itemMapper.selectByOrderId(10L)).thenReturn(Collections.singletonList(orderItem));
		when(skuMapper.selectBySkuCode("SKU-A")).thenReturn(sku);
		when(warehouseSkuCodeService.build(3L, "SKU-A")).thenReturn("JHIN-SKU-A");
		when(fulfillmentOrderService.createAndReserve(any())).thenReturn(99L);
		when(fulfillmentOrderService.statusOf(99L)).thenReturn(FulfillmentStatus.WAITING_PICK);
		when(orderMapper.updateById(any())).thenReturn(1);
		when(logisticsProductService.requireEnabledForOwner(8L, 3L)).thenReturn(product);
		TenantContext.setCurrentTenant(3L);
		WmsTenantContext.setCurrentWmsTenant(4L);
		SubmitFulfillmentDTO dto = new SubmitFulfillmentDTO();
		dto.setErpOrderId(10L);
		dto.setLogisticsProductId(8L);

		assertThat(service.submit(dto)).isEqualTo(99L);
		ArgumentCaptor<FulfillmentCreateCommand> captor = ArgumentCaptor.forClass(FulfillmentCreateCommand.class);
		verify(fulfillmentOrderService).createAndReserve(captor.capture());
		assertThat(captor.getValue().getWarehouseId()).isEqualTo(30L);
		assertThat(captor.getValue().getSourceType()).isEqualTo("OZON");
		assertThat(captor.getValue().getItems().get(0).getWarehouseSkuCode()).isEqualTo("JHIN-SKU-A");
		assertThat(captor.getValue().getLogisticsProductId()).isEqualTo(8L);
		assertThat(captor.getValue().getLogisticsProductName()).isEqualTo("加急派送");
		assertThat(captor.getValue().getLogisticsProductDefaultFee()).isEqualByComparingTo("48.00");
		assertThat(captor.getValue().getLogisticsProductActualFee()).isEqualByComparingTo("48.00");
		ArgumentCaptor<ErpOrder> updateCaptor = ArgumentCaptor.forClass(ErpOrder.class);
		verify(orderMapper).updateById(updateCaptor.capture());
		assertThat(updateCaptor.getValue().getWarehouseFulfillmentStatus()).isEqualTo("WAITING_PICK");
	}

	@Test
	void rejects_order_already_bound_to_new_or_legacy_outbound_flow() {
		ErpOrderMapper orderMapper = mock(ErpOrderMapper.class);
		ErpOrderFulfillmentSubmissionService service = new ErpOrderFulfillmentSubmissionService(orderMapper,
				mock(ErpOrderItemMapper.class), mock(ShopMapper.class), mock(SkuMapper.class),
				mock(WarehouseSkuCodeService.class), mock(WarehouseService.class),
				mock(FulfillmentOrderService.class), mock(WmsLogisticsProductService.class),
				new FulfillmentRecipientSnapshotService(new com.fasterxml.jackson.databind.ObjectMapper()));
		ErpOrder order = new ErpOrder();
		order.setId(11L);
		order.setShopId(20L);
		order.setFulfillmentType("FBS");
		order.setFulfillmentOrderId(88L);
		when(orderMapper.selectForFulfillmentSubmit(11L, 3L)).thenReturn(order);
		TenantContext.setCurrentTenant(3L);
		WmsTenantContext.setCurrentWmsTenant(4L);
		SubmitFulfillmentDTO dto = new SubmitFulfillmentDTO();
		dto.setErpOrderId(11L);

		assertThatThrownBy(() -> service.submit(dto))
				.hasMessageContaining("已经进入仓库履约或旧出库流程");

		order.setFulfillmentOrderId(null);
		order.setOutboundOrderId(99L);
		assertThatThrownBy(() -> service.submit(dto))
				.hasMessageContaining("已经进入仓库履约或旧出库流程");
	}
}
