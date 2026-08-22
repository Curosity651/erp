package com.erp.admin.wms;

import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.WmsLogisticsProductMapper;
import com.erp.admin.wms.model.entity.WmsLogisticsProduct;
import com.erp.admin.wms.service.WmsLogisticsProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WmsLogisticsProductServiceTest {

	private WmsLogisticsProductMapper mapper;
	private SysTenantMapper tenantMapper;
	private WmsLogisticsProductService service;

	@BeforeEach
	void setUp() {
		mapper = mock(WmsLogisticsProductMapper.class);
		tenantMapper = mock(SysTenantMapper.class);
		service = new WmsLogisticsProductService(mock(TenantIdentityService.class), tenantMapper);
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
	}

	@Test
	void owner_can_use_enabled_product_from_parent_wms() {
		when(tenantMapper.selectById(31L)).thenReturn(owner(9L));
		when(mapper.selectById(7L)).thenReturn(product(7L, 9L, 1));

		assertThat(service.requireEnabledForOwner(7L, 31L).getId()).isEqualTo(7L);
	}

	@Test
	void owner_cannot_use_product_from_another_wms() {
		when(tenantMapper.selectById(31L)).thenReturn(owner(9L));
		when(mapper.selectById(7L)).thenReturn(product(7L, 10L, 1));

		assertThatThrownBy(() -> service.requireEnabledForOwner(7L, 31L))
				.hasMessageContaining("不属于当前服务商");
	}

	@Test
	void disabled_product_cannot_be_selected() {
		when(tenantMapper.selectById(31L)).thenReturn(owner(9L));
		when(mapper.selectById(7L)).thenReturn(product(7L, 9L, 0));

		assertThatThrownBy(() -> service.requireEnabledForOwner(7L, 31L))
				.hasMessageContaining("不可用");
	}

	private SysTenant owner(Long parentId) {
		SysTenant owner = new SysTenant();
		owner.setId(31L);
		owner.setTenantType(TenantIdentityService.IDENTITY_ERP_USER);
		owner.setParentWmsTenantId(parentId);
		return owner;
	}

	private WmsLogisticsProduct product(Long id, Long wmsTenantId, Integer status) {
		WmsLogisticsProduct product = new WmsLogisticsProduct();
		product.setId(id);
		product.setWmsTenantId(wmsTenantId);
		product.setStatus(status);
		return product;
	}
}
