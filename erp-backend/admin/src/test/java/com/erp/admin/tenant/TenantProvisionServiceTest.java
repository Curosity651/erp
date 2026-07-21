package com.erp.admin.tenant;

import com.erp.admin.tenant.service.TenantProvisionService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 开通权限判定测试（B3 / 三平台改名）：只有海外仓平台能开服务商；只有服务商能开货主。
 *
 * @author erp
 */
class TenantProvisionServiceTest {

	@Test
	void only_overseas_platform_can_open_wms_operator() {
		assertThat(TenantProvisionService.canOpenWmsOperator("OVERSEAS_PLATFORM")).isTrue();
		assertThat(TenantProvisionService.canOpenWmsOperator("WMS_OPERATOR")).isFalse();
		assertThat(TenantProvisionService.canOpenWmsOperator("ERP_USER")).isFalse();
		assertThat(TenantProvisionService.canOpenWmsOperator(null)).isFalse();
	}

	@Test
	void only_wms_operator_can_open_erp_tenant() {
		assertThat(TenantProvisionService.canOpenErpTenant("WMS_OPERATOR")).isTrue();
		assertThat(TenantProvisionService.canOpenErpTenant("OVERSEAS_PLATFORM")).isFalse();
		assertThat(TenantProvisionService.canOpenErpTenant("ERP_USER")).isFalse();
		assertThat(TenantProvisionService.canOpenErpTenant(null)).isFalse();
	}

}
