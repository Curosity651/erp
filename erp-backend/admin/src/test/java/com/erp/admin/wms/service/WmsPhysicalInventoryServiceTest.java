package com.erp.admin.wms.service;

import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.wms.mapper.InventoryMapper;
import com.erp.admin.wms.mapper.StockFlowMapper;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WmsPhysicalInventoryServiceTest {

	private SysTenantMapper sysTenantMapper;

	private WmsPhysicalInventoryService service;

	@BeforeEach
	void setUp() {
		sysTenantMapper = mock(SysTenantMapper.class);
		service = new WmsPhysicalInventoryService(
				mock(WmsInventoryAggregator.class),
				mock(InventoryMapper.class),
				mock(StockFlowMapper.class),
				mock(PrincipalAttributeAccessor.class),
				mock(WmsPalletService.class),
				sysTenantMapper,
				mock(StocktakeFreezeService.class));
	}

	@Test
	void missingOrZeroProviderUsesOwnersProvider() {
		when(sysTenantMapper.selectById(6L)).thenReturn(owner(6L, 5L));

		assertEquals(5L, service.resolveWmsTenantId(6L, null));
		assertEquals(5L, service.resolveWmsTenantId(6L, 0L));
		assertEquals(5L, service.resolveWmsTenantId(6L, 5L));
	}

	@Test
	void mismatchedProviderIsRejected() {
		when(sysTenantMapper.selectById(6L)).thenReturn(owner(6L, 5L));

		assertThrows(IllegalArgumentException.class,
				() -> service.resolveWmsTenantId(6L, 7L));
	}

	private SysTenant owner(Long id, Long parentWmsTenantId) {
		SysTenant tenant = new SysTenant();
		tenant.setId(id);
		tenant.setParentWmsTenantId(parentWmsTenantId);
		return tenant;
	}

}
