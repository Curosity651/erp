package com.erp.admin.wms.service;

import com.erp.admin.order.service.ErpOrderService;
import com.erp.admin.order.service.ozon.OzonActService;
import com.erp.admin.product.service.SkuBarcodeService;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.wms.mapper.SalesOutboundItemMapper;
import com.erp.admin.wms.mapper.WmsOutboundScanEventMapper;
import com.erp.admin.wms.mapper.WmsSalesOutboundPackageMapper;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.entity.WmsSalesOutboundPackage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SalesOutboundPackageServiceTest {

	@Test
	void packScanContinuesWithNextIncompleteDuplicateSkuRow() {
		WmsSalesOutboundPackageMapper packageMapper = mock(WmsSalesOutboundPackageMapper.class);
		SalesOutboundItemMapper itemMapper = mock(SalesOutboundItemMapper.class);
		SkuBarcodeService barcodeService = mock(SkuBarcodeService.class);
		WmsOutboundScanEventMapper eventMapper = mock(WmsOutboundScanEventMapper.class);
		SalesOutboundPackageService service = new SalesOutboundPackageService(packageMapper, itemMapper,
				mock(ErpOrderService.class), mock(OzonActService.class), barcodeService,
                mock(WarehouseSkuCodeService.class), eventMapper,
				mock(PlatformLabelVerificationService.class));

		WmsSalesOutboundPackage pack = new WmsSalesOutboundPackage();
		pack.setId(1L);
		pack.setOutboundOrderId(10L);
		pack.setErpOrderId(20L);
		pack.setErpTenantId(6L);
		pack.setSortStatus(SalesOutboundPackageService.SORT_NOT_REQUIRED);
		pack.setPackStatus(SalesOutboundPackageService.PACK_PENDING);
		when(packageMapper.selectByIdForUpdate(1L)).thenReturn(pack);

		SalesOutboundOrderItem full = item(101L, "SKU-1", 2, 2);
		SalesOutboundOrderItem pending = item(102L, "SKU-1", 3, 0);
		when(itemMapper.selectPackageItemsForUpdate(10L, 20L)).thenReturn(Arrays.asList(full, pending));
		when(barcodeService.matches(6L, "SKU-1", "OWNER-SKU-1")).thenReturn(true);
		when(itemMapper.updateById(any())).thenReturn(1);
		when(eventMapper.insert(any())).thenReturn(1);

		service.scanPack(1L, "OWNER-SKU-1", 1, false, 9L, "operator");

		assertEquals(2, full.getPackedQuantity());
		assertEquals(1, pending.getPackedQuantity());
		verify(itemMapper).updateById(pending);
	}

	@Test
	void matching_platform_order_label_marks_package_as_attached() {
		WmsSalesOutboundPackageMapper packageMapper = mock(WmsSalesOutboundPackageMapper.class);
		WmsOutboundScanEventMapper eventMapper = mock(WmsOutboundScanEventMapper.class);
		PlatformLabelVerificationService verificationService = mock(PlatformLabelVerificationService.class);
		when(verificationService.matches(any(WmsSalesOutboundPackage.class), any())).thenReturn(true);
		SalesOutboundPackageService service = new SalesOutboundPackageService(packageMapper,
				mock(SalesOutboundItemMapper.class), mock(ErpOrderService.class), mock(OzonActService.class),
				mock(SkuBarcodeService.class), mock(WarehouseSkuCodeService.class), eventMapper,
				verificationService);
		WmsSalesOutboundPackage pack = pack(1L);
		pack.setPlatformOrderId("OZON-10001");
		pack.setLabelStatus(SalesOutboundPackageService.LABEL_READY);
		when(packageMapper.selectByIdForUpdate(1L)).thenReturn(pack);
		when(packageMapper.updateById(any())).thenReturn(1);
		when(eventMapper.insert(any())).thenReturn(1);

		service.confirmLabelAttached(1L, " OZON-10001 ", 9L, "operator");

		assertEquals(SalesOutboundPackageService.LABEL_ATTACHED_CONFIRMED, pack.getLabelStatus());
		verify(packageMapper).updateById(pack);
		verify(eventMapper).insert(any());
	}

	@Test
	void mismatching_platform_order_label_is_rejected() {
		WmsSalesOutboundPackageMapper packageMapper = mock(WmsSalesOutboundPackageMapper.class);
		SalesOutboundPackageService service = new SalesOutboundPackageService(packageMapper,
				mock(SalesOutboundItemMapper.class), mock(ErpOrderService.class), mock(OzonActService.class),
				mock(SkuBarcodeService.class), mock(WarehouseSkuCodeService.class),
				mock(WmsOutboundScanEventMapper.class), mock(PlatformLabelVerificationService.class));
		WmsSalesOutboundPackage pack = pack(1L);
		pack.setPlatformOrderId("OZON-10001");
		pack.setLabelStatus(SalesOutboundPackageService.LABEL_READY);
		when(packageMapper.selectByIdForUpdate(1L)).thenReturn(pack);

		assertThatThrownBy(() -> service.confirmLabelAttached(1L, "OZON-OTHER", 9L, "operator"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("不匹配");
	}

	@Test
	void picking_initialization_does_not_invent_physical_sort_slot_codes() {
		WmsSalesOutboundPackageMapper packageMapper = mock(WmsSalesOutboundPackageMapper.class);
		SalesOutboundItemMapper itemMapper = mock(SalesOutboundItemMapper.class);
		SalesOutboundPackageService service = new SalesOutboundPackageService(packageMapper, itemMapper,
				mock(ErpOrderService.class), mock(OzonActService.class), mock(SkuBarcodeService.class),
				mock(WarehouseSkuCodeService.class), mock(WmsOutboundScanEventMapper.class),
				mock(PlatformLabelVerificationService.class));
		WmsSalesOutboundPackage first = pack(1L);
		first.setSortCode("OLD-B01");
		first.setSortSlotId(99L);
		first.setSortSlotScanCode("OLD-SCAN");
		when(packageMapper.selectByOutboundOrderId(10L)).thenReturn(Arrays.asList(first));
		SalesOutboundOrderItem item = item(101L, "SKU-1", 2, 2);
		item.setSortedQuantity(2);
		when(itemMapper.selectByOutboundOrderId(10L)).thenReturn(Arrays.asList(item));
		when(packageMapper.updateById(any())).thenReturn(1);
		when(itemMapper.updateById(any())).thenReturn(1);

		service.prepareForPicking(10L, true);

		assertEquals(null, first.getSortCode());
		assertEquals(null, first.getSortSlotId());
		assertEquals(null, first.getSortSlotScanCode());
		assertEquals(SalesOutboundPackageService.SORT_PENDING, first.getSortStatus());
		assertEquals(0, item.getSortedQuantity());
		assertEquals(0, item.getPackedQuantity());
	}

	private static WmsSalesOutboundPackage pack(Long id) {
		WmsSalesOutboundPackage pack = new WmsSalesOutboundPackage();
		pack.setId(id);
		return pack;
	}

	private static SalesOutboundOrderItem item(Long id, String skuCode, int quantity, int packedQuantity) {
		SalesOutboundOrderItem item = new SalesOutboundOrderItem();
		item.setId(id);
		item.setSkuCode(skuCode);
		item.setQuantity(quantity);
		item.setPackedQuantity(packedQuantity);
		return item;
	}

}
