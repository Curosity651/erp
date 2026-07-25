package com.erp.admin.tenant;

import com.erp.admin.common.tenant.ErpTenantLineHandler;
import com.erp.admin.common.tenant.TenantContext;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多租户行级隔离处理器测试（A2）。 验证方案②：白名单表 + 有上下文才注入；无上下文跳过；非白名单表始终跳过。
 *
 * @author erp
 */
class ErpTenantLineHandlerTest {

	private final ErpTenantLineHandler handler = new ErpTenantLineHandler();

	@AfterEach
	void tearDown() {
		TenantContext.clear();
	}

	@Test
	void whitelisted_table_with_context_is_scoped() {
		TenantContext.setCurrentTenant(1L);

		assertThat(handler.ignoreTable("sku")).isFalse();
		assertThat(handler.ignoreTable("SKU")).isFalse(); // 大小写不敏感
		assertThat(handler.getTenantIdColumn()).isEqualTo("tenant_id");
		assertThat(((LongValue) handler.getTenantId()).getValue()).isEqualTo(1L);
	}

	@Test
	void business_table_without_context_is_fail_closed() {
		// 方案②：后台无上下文线程不注入，避免打断定时任务
		assertThat(TenantContext.getCurrentTenant()).isNull();
		assertThat(handler.ignoreTable("sku")).isFalse();
		assertThat(handler.ignoreTable("sku_mapping")).isFalse();
	}

	@Test
	void non_whitelisted_table_always_skipped() {
		TenantContext.setCurrentTenant(1L);

		// 全局共享 / 系统表 / WMS 物理表均不纳入 ERP 租户隔离
		assertThat(handler.ignoreTable("exchange_rate")).isTrue();
		assertThat(handler.ignoreTable("sys_menu")).isTrue(); // 菜单仍全局共享
		assertThat(handler.ignoreTable("sys_dict")).isTrue(); // 字典/配置全局共享
		assertThat(handler.ignoreTable("sys_config")).isTrue();
		assertThat(handler.ignoreTable("sys_tenant")).isTrue();
		assertThat(handler.ignoreTable("wms_inventory")).isTrue();
		assertThat(handler.ignoreTable(null)).isTrue();
	}

	@Test
	void identity_tables_are_scoped_with_context() {
		// V19 sys_role / V20 sys_user、sys_organization 纳入租户隔离（三平台身份隔离）；无上下文仍跳过
		TenantContext.setCurrentTenant(2L);
		assertThat(handler.ignoreTable("sys_role")).isFalse();
		assertThat(handler.ignoreTable("SYS_ROLE")).isFalse(); // 大小写不敏感
		assertThat(handler.ignoreTable("sys_user")).isFalse();
		assertThat(handler.ignoreTable("sys_organization")).isFalse();
		assertThat(((LongValue) handler.getTenantId()).getValue()).isEqualTo(2L);

		TenantContext.clear();
		assertThat(handler.ignoreTable("sys_role")).isTrue();
		assertThat(handler.ignoreTable("sys_user")).isTrue();
		assertThat(handler.ignoreTable("sys_organization")).isTrue();
	}

	@Test
	void whitelisted_tables_are_scoped() {
		TenantContext.setCurrentTenant(9L);
		String[] tables = {
				// A2 商品
				"sku", "category", "brand", "supplier", "sku_mapping", "sku_files", "sku_barcode",
				// A3 ERP/OMS 业务
				"erp_order", "erp_order_item", "erp_label_batch", "erp_label_batch_file", "erp_label_batch_item",
				"ozon_shipment_act", "ozon_shipment_act_order", "ozon_delivery_method_rule", "shop",
				"project_group", "position", "sales_target", "sync_cursor", "wb_report_detail", "wb_financial_sync_job",
				"wb_financial_sync_task", "wb_financial_sync_page_log", "wb_office", "wb_supply" };
		for (String t : tables) {
			assertThat(handler.ignoreTable(t)).as("table %s should be scoped", t).isFalse();
		}
	}

}
