package com.erp.admin.wms.datascope;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.erp.admin.wms.service.ErpOwnerScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.springframework.stereotype.Component;

/**
 * 货主单据「按 erp_tenant_id 隔离」数据权限处理器（Bug1 修复，数据级可见性）。
 *
 * <p>对下列带 {@code erp_tenant_id} 的货主业务表的 <b>SELECT</b>（以及 update/delete 的 WHERE）自动注入
 * {@code erp_tenant_id} 范围条件，使：货主只见自己；WMS 服务商只见名下货主（只读）；海外仓平台看全部（不过滤）。
 *
 * <ul>
 * <li>{@code wms_logistics_provider} 物流商</li>
 * <li>{@code wms_purchase_order} 采购单</li>
 * <li>{@code wms_shipping_order} 物流单</li>
 * <li>{@code wms_sales_outbound_order} 销售/自定义出库单</li>
 * <li>{@code wms_return_inbound_order} 退货/自定义退货单</li>
 * <li>{@code wms_inventory_config} 库存配置</li>
 * </ul>
 *
 * <p>作用域由 {@link ErpOwnerScopeService#readScope()} 给出：返回 {@code null} 时不加任何条件（平台/无上下文→看全部，
 * 故平台的收货/上架回写按主键操作不受影响）。已隔离的入库单/库存/批次/流水由各自服务显式处理，<b>不</b>纳入本处理器，避免重复。
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WmsOwnerDataPermissionHandler implements MultiDataPermissionHandler {

	/** 纳入 erp_tenant_id 隔离的货主业务表（小写）。 */
	private static final Set<String> OWNER_TABLES = new HashSet<>(java.util.Arrays.asList(
		"wms_logistics_provider", "wms_purchase_order", "wms_shipping_order",
		"wms_sales_outbound_order", "wms_return_inbound_order", "wms_inventory_config",
		"wms_region_inventory"));

	private final ErpOwnerScopeService erpOwnerScopeService;

	@Override
	public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
		if (table == null || table.getName() == null) {
			return null;
		}
		String name = table.getName().replace("`", "").toLowerCase();
		if (!OWNER_TABLES.contains(name)) {
			// 非货主隔离表：不加限制
			return null;
		}
		List<Long> scope = erpOwnerScopeService.readScope();
		if (scope == null) {
			// 海外仓平台 / 无上下文：看全部
			return null;
		}
		// 用表别名（或表名）限定列，兼容 JOIN/别名场景
		String prefix = (table.getAlias() != null) ? table.getAlias().getName() : table.getName();
		try {
			if (scope.size() == 1) {
				EqualsTo eq = new EqualsTo();
				eq.setLeftExpression(new Column(new Table(prefix), "erp_tenant_id"));
				eq.setRightExpression(new LongValue(scope.get(0)));
				return eq;
			}
			String ids = scope.stream().map(String::valueOf).collect(Collectors.joining(","));
			return CCJSqlParserUtil.parseCondExpression(prefix + ".erp_tenant_id IN (" + ids + ")");
		}
		catch (Exception e) {
			// 解析异常：fail-closed 屏蔽，避免越权可见
			log.warn("货主数据权限条件构建失败，按屏蔽处理，table={}", name, e);
			EqualsTo block = new EqualsTo();
			block.setLeftExpression(new LongValue(1));
			block.setRightExpression(new LongValue(2));
			return block;
		}
	}

}
