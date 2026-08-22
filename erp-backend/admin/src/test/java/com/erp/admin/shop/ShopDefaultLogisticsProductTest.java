package com.erp.admin.shop;

import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.shop.mapper.ShopMapper;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.model.vo.ShopDetailVO;
import com.erp.admin.shop.service.ShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShopDefaultLogisticsProductTest {

	private ShopMapper mapper;
	private ShopService service;

	@BeforeEach
	void setUp() {
		mapper = mock(ShopMapper.class);
		service = new ShopService(mock(CredentialService.class));
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
	}

	@Test
	void detail_returns_saved_default_logistics_product() {
		Shop shop = new Shop();
		shop.setId(5L);
		shop.setCredential("{}");
		shop.setDefaultLogisticsProductId(7L);
		when(mapper.selectById(5L)).thenReturn(shop);

		ShopDetailVO detail = service.detail(5L);

		assertThat(detail.getDefaultLogisticsProductId()).isEqualTo(7L);
	}
}
