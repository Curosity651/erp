package com.erp.admin.tenant;

import com.erp.admin.tenant.service.TenantIdentityService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 登录入口匹配校验测试（B2）。
 *
 * @author erp
 */
class TenantIdentityServiceTest {

	@Test
	void blank_expect_type_skips_check() {
		// 不传入口类型 → 不校验，始终匹配
		assertThat(TenantIdentityService.entryMatches(null, "ERP_USER")).isTrue();
		assertThat(TenantIdentityService.entryMatches("", "WMS_OPERATOR")).isTrue();
		assertThat(TenantIdentityService.entryMatches("   ", "ERP_USER")).isTrue();
	}

	@Test
	void matching_entry_passes() {
		assertThat(TenantIdentityService.entryMatches("ERP_USER", "ERP_USER")).isTrue();
		assertThat(TenantIdentityService.entryMatches("WMS_OPERATOR", "WMS_OPERATOR")).isTrue();
		assertThat(TenantIdentityService.entryMatches("OVERSEAS_PLATFORM", "OVERSEAS_PLATFORM")).isTrue();
	}

	@Test
	void mismatching_entry_rejected() {
		// 三入口两两错配 → 拒绝
		assertThat(TenantIdentityService.entryMatches("WMS_OPERATOR", "ERP_USER")).isFalse();
		assertThat(TenantIdentityService.entryMatches("ERP_USER", "WMS_OPERATOR")).isFalse();
		assertThat(TenantIdentityService.entryMatches("OVERSEAS_PLATFORM", "ERP_USER")).isFalse();
		assertThat(TenantIdentityService.entryMatches("ERP_USER", "OVERSEAS_PLATFORM")).isFalse();
		assertThat(TenantIdentityService.entryMatches("WMS_OPERATOR", "OVERSEAS_PLATFORM")).isFalse();
	}

}
