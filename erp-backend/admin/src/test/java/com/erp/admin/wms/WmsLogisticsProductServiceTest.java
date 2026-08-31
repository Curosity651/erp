package com.erp.admin.wms;

import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.WmsLogisticsProductMapper;
import com.erp.admin.wms.model.dto.LogisticsProductDTO;
import com.erp.admin.wms.model.entity.WmsLogisticsProduct;
import com.erp.admin.wms.service.WmsLogisticsProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WmsLogisticsProductServiceTest {

	private WmsLogisticsProductMapper mapper;
	private SysTenantMapper tenantMapper;
	private TenantIdentityService identityService;
	private WmsLogisticsProductService service;

	@BeforeEach
	void setUp() {
		mapper = mock(WmsLogisticsProductMapper.class);
		tenantMapper = mock(SysTenantMapper.class);
		identityService = mock(TenantIdentityService.class);
		TenantIdentityVO identity = new TenantIdentityVO();
		identity.setIdentityType(TenantIdentityService.IDENTITY_WMS_OPERATOR);
		when(identityService.currentIdentity(null)).thenReturn(identity);
		WmsTenantContext.setCurrentWmsTenant(9L);
		service = new WmsLogisticsProductService(identityService, tenantMapper);
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
	}

	@AfterEach
	void tearDown() {
		WmsTenantContext.clear();
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

	@Test
	void product_code_is_required_and_normalized_for_creation() {
		LogisticsProductDTO blank = dto(" ");
		assertThatThrownBy(() -> service.createProduct(blank)).hasMessageContaining("产品编码不能为空");

		LogisticsProductDTO valid = dto(" small-eco ");
		when(mapper.countByCode(9L, "SMALL-ECO", null)).thenReturn(0L);
		service.createProduct(valid);

		org.mockito.ArgumentCaptor<WmsLogisticsProduct> captor = org.mockito.ArgumentCaptor
			.forClass(WmsLogisticsProduct.class);
		verify(mapper).insert(captor.capture());
		assertThat(captor.getValue().getProductCode()).isEqualTo("SMALL-ECO");
	}

	@Test
	void duplicate_product_code_is_rejected_within_the_same_provider() {
		when(mapper.countByCode(9L, "STANDARD", null)).thenReturn(1L);

		assertThatThrownBy(() -> service.createProduct(dto("STANDARD")))
			.hasMessageContaining("产品编码已存在");
	}

	@Test
	void logistics_product_cannot_be_deleted_even_when_unused() {
		when(mapper.selectById(7L)).thenReturn(product(7L, 9L, 1));

		assertThatThrownBy(() -> service.deleteProduct(7L))
			.hasMessageContaining("不能删除")
			.hasMessageContaining("停用");
	}

	@Test
	void used_product_cannot_change_code_price_or_currency() {
		WmsLogisticsProduct existing = product(7L, 9L, 1);
		existing.setProductCode("STANDARD");
		existing.setProductName("标准物流服务");
		existing.setUnitPrice(java.math.BigDecimal.TEN);
		existing.setCurrency("RUB");
		when(mapper.selectById(7L)).thenReturn(existing);
		when(mapper.countFulfillmentReferences(9L, 7L)).thenReturn(1L);

		LogisticsProductDTO changed = dto("STANDARD-V2");
		changed.setUnitPrice(java.math.BigDecimal.valueOf(20));
		changed.setCurrency("CNY");

		assertThatThrownBy(() -> service.updateProduct(7L, changed))
			.hasMessageContaining("已产生业务数据")
			.hasMessageContaining("复制调价");
	}

	@Test
	void used_product_can_update_name_and_description_without_changing_pricing_identity() {
		WmsLogisticsProduct existing = product(7L, 9L, 1);
		existing.setProductCode("STANDARD");
		existing.setProductName("旧名称");
		existing.setUnitPrice(java.math.BigDecimal.TEN);
		existing.setCurrency("RUB");
		when(mapper.selectById(7L)).thenReturn(existing);
		when(mapper.countFulfillmentReferences(9L, 7L)).thenReturn(1L);
		when(mapper.countByCode(9L, "STANDARD", 7L)).thenReturn(0L);

		LogisticsProductDTO changed = dto("STANDARD");
		changed.setProductName("新名称");
		changed.setProductDescription("更新后的说明");
		service.updateProduct(7L, changed);

		assertThat(existing.getProductName()).isEqualTo("新名称");
		assertThat(existing.getUnitPrice()).isEqualByComparingTo(java.math.BigDecimal.TEN);
		verify(mapper).updateById(existing);
	}

	@Test
	void product_used_as_shop_default_cannot_be_disabled() {
		when(mapper.selectById(7L)).thenReturn(product(7L, 9L, 1));
		when(mapper.countShopReferences(9L, 7L)).thenReturn(2L);

		assertThatThrownBy(() -> service.updateStatus(7L, 0))
			.hasMessageContaining("2 个店铺")
			.hasMessageContaining("默认物流产品");
	}

	private LogisticsProductDTO dto(String code) {
		LogisticsProductDTO dto = new LogisticsProductDTO();
		dto.setProductName("标准物流服务");
		dto.setProductCode(code);
		dto.setUnitPrice(java.math.BigDecimal.TEN);
		dto.setCurrency("RUB");
		return dto;
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
