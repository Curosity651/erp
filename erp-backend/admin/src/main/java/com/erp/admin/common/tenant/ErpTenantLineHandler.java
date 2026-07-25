package com.erp.admin.common.tenant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

/**
 * ERP+OMS 多租户行级隔离处理器。
 *
 * <p><b>白名单策略</b>：只对 {@link #APPLIED_TABLES} 中的表自动注入 {@code WHERE tenant_id = ?}， 其余表一律
 * {@code ignoreTable=true} 不注入。
 *
 * <p><b>逐步启用</b>：A1 白名单为空（忽略所有表）；A2 起按模块把已加 {@code tenant_id} 列的表加入
 * {@link #APPLIED_TABLES}。A2 纳入商品族表。
 *
 * <p><b>无上下文策略（方案②）</b>：白名单表仅在「当前线程有租户上下文」时注入 {@code WHERE tenant_id}；
 * 无上下文的线程（如订单同步等后台定时/异步任务）则跳过注入，避免打断后台作业。 严格的「Web 无上下文即拒绝」留待 B1
 * 引入系统上下文后再收紧。
 *
 * <p>WMS 物理表不在此处理，由 WMS Mapper 用 {@code @InterceptorIgnore(tenantLine="true")} 绕过， 并显式按
 * {@link WmsTenantContext} 过滤。
 *
 * @author erp
 */
public class ErpTenantLineHandler implements TenantLineHandler {

	/**
	 * 启用行级租户注入的表白名单（小写）。
	 *
	 * <p>A2：商品族表。A3：其余 ERP/OMS 货主业务表（订单/店铺/贴标/统计辅助/WB财务）。
	 * <p>V19：纳入 {@code sys_role}（角色租户化）；V20：纳入 {@code sys_user}/{@code sys_organization}（三平台用户/组织隔离）。
	 * 原生角色/用户/组织管理据此按 tenant_id 自动隔离：各实例只看/只改自己的用户、角色、部门；新建时 INSERT 自动回填 tenant_id。
	 * 登录阶段无上下文（{@code ignoreTable} 跳过）不受影响；按用户名全局解析/唯一校验用 {@code @InterceptorIgnore} 绕过。
	 * <p>其余 {@code sys_*}（菜单/字典/配置等）仍全局共享，不在此注入。
	 * <p>不含：{@code exchange_rate}、其余 {@code sys_*}（全局共享）、{@code wms_*}（C/D 阶段用 wms_tenant_id/erp_tenant_id 另行处理）。
	 */
	private static final Set<String> APPLIED_TABLES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		// V19 角色租户化 / V20 用户·组织隔离
		"sys_role", "sys_user", "sys_organization",
		// V22 文件隔离(P8) + 日志按租户(P4：写入自动填 tenant_id、读取自动按租户过滤=同租户可见)
		"sys_file", "export_data", "log_login_log", "log_operation_log", "log_access_log",
		// A2 商品模块
		"sku", "category", "brand", "supplier", "sku_mapping", "sku_files", "sku_barcode",
		// A3 订单
		"erp_order", "erp_order_item",
		// A3 贴标
		"erp_label_batch", "erp_label_batch_file", "erp_label_batch_item",
		// Ozon 运单(交接单 act)：货主维度隔离，无租户上下文时 fail-closed
		"ozon_shipment_act", "ozon_shipment_act_order",
		"ozon_delivery_method_rule",
		// A3 店铺
		"shop",
		// A3 系统辅助
		"project_group", "position", "sales_target", "sync_cursor",
		// A3 WB 财务
		"wb_report_detail", "wb_financial_sync_job", "wb_financial_sync_task", "wb_financial_sync_page_log",
		// A3 WB 业务
		"wb_office", "wb_supply")));

	/**
	 * 无租户上下文时【仍放行】（不注入）的表——C2 fail-closed 的例外集。
	 *
	 * <p>登录/框架按用户名跨租户加载用户·角色·组织（ballcat 认证发生在租户上下文建立之前），
	 * 以及匿名/登录前的日志·文件写入（无归属，保持 NULL 不误挂），这些无上下文访问是<b>合法</b>的，
	 * 若也 fail-closed 会打断登录与日志。故认证/日志/文件类表在无上下文时放行（有上下文仍正常按租户注入、隔离不变）。
	 * <p>真正需要收紧的是<b>业务数据表</b>（商品/订单/店铺/WB 等）——它们无上下文即 fail-closed(-999→0 行)，
	 * 合法的后台跨租户扫描(shop/wb_financial_sync_task)另由 {@code runAsPlatformScan} 显式放行。
	 */
	private static final Set<String> NO_CONTEXT_ALLOW = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		"sys_user", "sys_role", "sys_organization",
		"sys_file", "export_data", "log_login_log", "log_operation_log", "log_access_log")));

	@Override
	public Expression getTenantId() {
		Long tenantId = TenantContext.getCurrentTenant();
		if (tenantId == null) {
			// C2：无上下文时只有【业务表】会走到这里（认证/日志/文件表已在 ignoreTable 的 NO_CONTEXT_ALLOW 放行，
			// 平台扫描也已放行）。对业务表 fail-closed——注入 UNBOUND 哨兵(-999) → SELECT 命中 0 行，
			// 堵住 S1 位置③ 的"无上下文读全部租户"。用哨兵而非抛异常：漏放行的后台读取退化为"安全的 0 行"而非崩溃。
			return new LongValue(TenantContext.UNBOUND_TENANT_ID);
		}
		return new LongValue(tenantId);
	}

	@Override
	public String getTenantIdColumn() {
		return "tenant_id";
	}

	/**
	 * 是否忽略该表的租户注入。
	 * @param tableName 表名
	 * @return true=忽略（不注入）
	 */
	@Override
	public boolean ignoreTable(String tableName) {
		if (tableName == null) {
			return true;
		}
		String name = tableName.toLowerCase();
		if (!APPLIED_TABLES.contains(name)) {
			return true;
		}
		// 合法的平台级跨租户扫描（后台同步拉全量店铺/财务任务）：显式放行。
		if (TenantContext.isPlatformScan()) {
			return true;
		}
		// 有租户上下文：正常注入（按当前租户隔离，行为不变）。
		if (TenantContext.getCurrentTenant() != null) {
			return false;
		}
		// 无上下文：认证/日志/文件类表放行（登录跨租户加载用户、匿名日志写 NULL 需要）；
		// 其余业务表 fail-closed —— 由 getTenantId 注入 -999 → SELECT 0 行、堵住 S1 位置③ 的跨租户读全部。
		return NO_CONTEXT_ALLOW.contains(name);
	}

}
