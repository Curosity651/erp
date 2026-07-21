package com.erp.admin.tenant;

import com.erp.admin.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 租户上下文单元测试（A1）
 *
 * @author erp
 */
class TenantContextTest {

	@AfterEach
	void tearDown() {
		TenantContext.clear();
	}

	@Test
	void set_get_clear() {
		assertThat(TenantContext.getCurrentTenant()).isNull();

		TenantContext.setCurrentTenant(7L);
		assertThat(TenantContext.getCurrentTenant()).isEqualTo(7L);

		TenantContext.clear();
		assertThat(TenantContext.getCurrentTenant()).isNull();
	}

	@Test
	void runAs_restores_previous_value() {
		TenantContext.setCurrentTenant(1L);

		String result = TenantContext.runAs(2L, () -> {
			assertThat(TenantContext.getCurrentTenant()).isEqualTo(2L);
			return "done";
		});

		assertThat(result).isEqualTo("done");
		// 嵌套执行后恢复为外层值
		assertThat(TenantContext.getCurrentTenant()).isEqualTo(1L);
	}

	@Test
	void runAs_clears_when_no_previous_context() {
		assertThat(TenantContext.getCurrentTenant()).isNull();

		TenantContext.runAs(5L, () -> {
			assertThat(TenantContext.getCurrentTenant()).isEqualTo(5L);
			return null;
		});

		// 原本无上下文，执行后应清空而非残留
		assertThat(TenantContext.getCurrentTenant()).isNull();
	}

}
