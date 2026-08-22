package com.erp.admin.wms;

import java.util.Arrays;
import java.util.Collections;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.wms.mapper.WmsFulfillmentItemMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.model.dto.FulfillmentCreateCommand;
import com.erp.admin.wms.model.dto.ManualFulfillmentDTO;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import com.erp.admin.wms.service.FulfillmentReservationService;
import com.erp.admin.wms.service.ManualFulfillmentService;
import com.erp.admin.wms.service.WarehouseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManualFulfillmentServiceTest {
	private WmsFulfillmentOrderMapper orderMapper;
	private WmsFulfillmentItemMapper itemMapper;
	private SkuMapper skuMapper;
	private FulfillmentReservationService reservationService;
	private ManualFulfillmentService service;

	@BeforeEach
	void setUp() {
		orderMapper = mock(WmsFulfillmentOrderMapper.class);
		itemMapper = mock(WmsFulfillmentItemMapper.class);
		skuMapper = mock(SkuMapper.class);
		WarehouseSkuCodeService codeService = mock(WarehouseSkuCodeService.class);
		WarehouseService warehouseService = mock(WarehouseService.class);
		reservationService = mock(FulfillmentReservationService.class);
		service = new ManualFulfillmentService(orderMapper, itemMapper, skuMapper, codeService,
				warehouseService, reservationService);
		TenantContext.setCurrentTenant(3L);
		WmsTenantContext.setCurrentWmsTenant(4L);
		when(orderMapper.insert(any())).thenAnswer(invocation -> {
			WmsFulfillmentOrder order = invocation.getArgument(0);
			order.setId(91L);
			return 1;
		});
		when(itemMapper.insert(any())).thenReturn(1);
		when(codeService.build(3L, "SKU-A")).thenReturn("JHIN-SKU-A");
		when(skuMapper.selectBySkuCode("SKU-A")).thenReturn(sku("SKU-A"));
	}

	@AfterEach
	void clearTenant() {
		TenantContext.clear();
		WmsTenantContext.clear();
	}

	@Test
	void saves_one_package_draft_with_sku_snapshot() {
		Long id = service.saveDraft(dto(Collections.singletonList(item("SKU-A", 2))));

		assertThat(id).isEqualTo(91L);
		ArgumentCaptor<WmsFulfillmentOrder> order = ArgumentCaptor.forClass(WmsFulfillmentOrder.class);
		verify(orderMapper).insert(order.capture());
		assertThat(order.getValue().getSourceType()).isEqualTo("MANUAL");
		assertThat(order.getValue().getFulfillmentStatus()).isEqualTo(FulfillmentStatus.DRAFT);
	}

	@Test
	void submits_draft_by_reserving_inventory_before_state_transition() {
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(91L);
		order.setTenantId(1L);
		order.setWmsTenantId(4L);
		order.setErpTenantId(3L);
		order.setWarehouseId(5L);
		order.setSourceType("MANUAL");
		order.setSourceOrderId(20L);
		order.setSourceOrderNo("MO-20");
		order.setFulfillmentStatus(FulfillmentStatus.DRAFT);
		when(orderMapper.selectForUpdate(91L)).thenReturn(order);
		when(itemMapper.selectList(any())).thenReturn(Collections.singletonList(snapshot()));
		when(orderMapper.transit(91L, FulfillmentStatus.DRAFT, FulfillmentStatus.WAITING_SHELF)).thenReturn(1);

		service.submit(91L);

		verify(reservationService).reserve(any(Long.class), any(FulfillmentCreateCommand.class), any());
		verify(orderMapper).transit(91L, FulfillmentStatus.DRAFT, FulfillmentStatus.WAITING_SHELF);
	}

	@Test
	void rejects_duplicate_sku_lines_before_creating_draft() {
		ManualFulfillmentDTO dto = dto(Arrays.asList(item("SKU-A", 1), item("SKU-A", 2)));

		assertThatThrownBy(() -> service.saveDraft(dto)).hasMessageContaining("SKU不能重复");
		verify(orderMapper, never()).insert(any());
	}

	private ManualFulfillmentDTO dto(java.util.List<ManualFulfillmentDTO.Item> items) {
		ManualFulfillmentDTO dto = new ManualFulfillmentDTO();
		dto.setWarehouseId(5L);
		dto.setRecipientName("Receiver");
		dto.setItems(items);
		return dto;
	}

	private ManualFulfillmentDTO.Item item(String skuCode, int quantity) {
		ManualFulfillmentDTO.Item item = new ManualFulfillmentDTO.Item();
		item.setSkuCode(skuCode);
		item.setQuantity(quantity);
		return item;
	}

	private Sku sku(String skuCode) {
		Sku sku = new Sku();
		sku.setSkuCode(skuCode);
		sku.setChineseName("Test SKU");
		sku.setOuterLengthMm(100);
		sku.setOuterWidthMm(200);
		sku.setOuterHeightMm(300);
		sku.setOuterGrossWeightG(4000);
		return sku;
	}

	private com.erp.admin.wms.model.entity.WmsFulfillmentItem snapshot() {
		com.erp.admin.wms.model.entity.WmsFulfillmentItem item = new com.erp.admin.wms.model.entity.WmsFulfillmentItem();
		item.setSkuCode("SKU-A");
		item.setWarehouseSkuCode("JHIN-SKU-A");
		item.setQuantity(2);
		item.setQuality("GOOD");
		item.setOuterLengthMm(100);
		item.setOuterWidthMm(200);
		item.setOuterHeightMm(300);
		item.setOuterGrossWeightG(4000);
		return item;
	}
}
