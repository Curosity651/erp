package com.erp.admin.product;

import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WarehouseSkuCodeServiceTest {

	private final SysTenantMapper tenantMapper = mock(SysTenantMapper.class);

	private final WarehouseSkuCodeService service = new WarehouseSkuCodeService(tenantMapper);

	@Test
	void buildsNormalizedOwnerScopedSkuCode() {
		SysTenant owner = owner(10L, "OWNER_TECH_CODE", " jhin ");
		when(tenantMapper.selectById(10L)).thenReturn(owner);

		assertEquals("JHIN-ADNZ-015-TXW", service.build(10L, " ADNZ-015-TXW "));
	}

	@Test
	void matchesWarehouseSkuCodeIgnoringCase() {
		when(tenantMapper.selectById(10L)).thenReturn(owner(10L, "OWNER_TECH_CODE", "JHIN"));

		assertTrue(service.matches(10L, "ADNZ-015-TXW", "jhin-adnz-015-txw"));
		assertFalse(service.matches(10L, "ADNZ-015-TXW", "OTHER-ADNZ-015-TXW"));
	}

	@Test
	void rejectsOwnerWithoutName() {
		when(tenantMapper.selectById(10L)).thenReturn(owner(10L, "OWNER_TECH_CODE", " "));

		assertThrows(IllegalArgumentException.class, () -> service.build(10L, "SKU-1"));
	}

	@Test
	void missingOwnerNameDoesNotBreakOtherBarcodeMatchingPaths() {
		when(tenantMapper.selectById(10L)).thenReturn(owner(10L, "OWNER_TECH_CODE", " "));

		assertFalse(service.matches(10L, "SKU-1", "SOME-EAN-CODE"));
	}

	private static SysTenant owner(Long id, String code, String name) {
		SysTenant tenant = new SysTenant();
		tenant.setId(id);
		tenant.setTenantCode(code);
		tenant.setTenantName(name);
		return tenant;
	}

}
