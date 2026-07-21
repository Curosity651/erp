package com.erp.admin.tenant;

import com.erp.admin.common.tenant.TenantResolver;
import com.erp.admin.tenant.model.entity.SysTenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 租户路由解析测试（B1）。验证「平台屏蔽货主 OMS」决策下的双层路由。
 *
 * @author erp
 */
class TenantResolverTest {

	private static SysTenant tenant(Long id, String type, Long parentWmsId) {
		SysTenant t = new SysTenant();
		t.setId(id);
		t.setTenantType(type);
		t.setParentWmsTenantId(parentWmsId);
		return t;
	}

	@Test
	void erp_user_scopes_oms_to_self_and_wms_to_parent() {
		TenantResolver.Resolution r = TenantResolver.resolve(tenant(1L, "ERP_USER", 2L));

		// 货主：OMS 表按自己；WMS 表按所属平台
		assertThat(r.getErpTenantId()).isEqualTo(1L);
		assertThat(r.getWmsTenantId()).isEqualTo(2L);
		assertThat(r.isEmpty()).isFalse();
	}

	@Test
	void wms_operator_scopes_both_to_self() {
		TenantResolver.Resolution r = TenantResolver.resolve(tenant(2L, "WMS_OPERATOR", null));

		// 平台：OMS 表按自己(无数据→屏蔽货主)；WMS 表按自己(看名下所有货主)
		assertThat(r.getErpTenantId()).isEqualTo(2L);
		assertThat(r.getWmsTenantId()).isEqualTo(2L);
	}

	@Test
	void null_tenant_resolves_to_none() {
		TenantResolver.Resolution r = TenantResolver.resolve(null);

		assertThat(r.getErpTenantId()).isNull();
		assertThat(r.getWmsTenantId()).isNull();
		assertThat(r.isEmpty()).isTrue();
	}

	@Test
	void unknown_type_resolves_to_none() {
		TenantResolver.Resolution r = TenantResolver.resolve(tenant(9L, "SOMETHING_ELSE", 1L));

		assertThat(r.isEmpty()).isTrue();
	}

}
