package com.erp.admin.wms.service;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.wms.mapper.PurchaseInboundItemMapper;
import com.erp.admin.wms.model.vo.PurchaseInboundItemVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PurchaseInboundItemServiceTest {

	@AfterEach
	void clearTenant() {
		TenantContext.clear();
	}

	@Test
	void enrichesInboundSkuInsideOwnerTenantContext() {
		PurchaseInboundItemMapper mapper = mock(PurchaseInboundItemMapper.class);
		SkuBriefService briefService = mock(SkuBriefService.class);
		WarehouseSkuCodeService warehouseSkuCodeService = mock(WarehouseSkuCodeService.class);
		PurchaseInboundItemService service = new PurchaseInboundItemService(briefService, warehouseSkuCodeService);
		ReflectionTestUtils.setField(service, "baseMapper", mapper);

		PurchaseInboundItemVO item = new PurchaseInboundItemVO();
		item.setSkuCode("SKU-1");
		when(mapper.selectItemVOsByInboundOrderId(10L, 6L)).thenReturn(Collections.singletonList(item));
		when(warehouseSkuCodeService.build(6L, "SKU-1")).thenReturn("JHIN-SKU-1");
		doAnswer(invocation -> {
			assertEquals(6L, TenantContext.getCurrentTenant());
			return null;
		}).when(briefService).enrichForQuery(any(), any(), any());

		service.getVoListByInboundOrderId(10L, 6L);

		assertEquals("JHIN-SKU-1", item.getWarehouseSkuCode());
	}

}
