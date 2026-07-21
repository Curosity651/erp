package com.erp.admin.wms.service;

import java.util.Collections;
import java.util.List;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.tenant.enums.TenantResultCode;
import com.erp.admin.tenant.exception.TenantBusinessException;
import com.erp.admin.tenant.service.TenantHierarchyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 货主单据（采购单/物流单/出库/退货/物流商/库存配置等带 {@code erp_tenant_id} 的 wms 业务表）的数据级作用域解析。
 *
 * <p>三平台可见性规则（用户拍板）：
 * <ul>
 * <li><b>货主 ERP_USER</b>：仅自己的单据（读写）。</li>
 * <li><b>WMS 服务商</b>：其名下货主的单据，<b>只读</b>（写操作禁止）。</li>
 * <li><b>海外仓平台</b>：全部（读；物理收货/上架作业按主键显式操作，不经本作用域）。</li>
 * </ul>
 *
 * <p>身份判定用线程本地上下文（避免每次查库）：海外仓平台 {@code TenantContext == BLOCK(-1)}；
 * 服务商 {@code WmsTenantContext == TenantContext}（其 wms 维与 erp 维同为自身 id）；其余为货主。
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class ErpOwnerScopeService {

	/** 服务商名下无货主时的占位 id，使 IN 查询命中 0 行（而非退化为看全部）。 */
	private static final Long BLOCK_ID = -999L;

	private final TenantHierarchyService tenantHierarchyService;

	/**
	 * 读作用域：返回允许查看的 {@code erp_tenant_id} 集合；返回 {@code null} 表示不过滤（看全部，仅平台）。
	 * <p>S1 收敛：区分「平台」与「无上下文」——只有海外仓平台(-1)才放行看全部；无上下文（漏设租户的线程/异常路径）
	 * 一律返回占位 id 命中 0 行（fail-closed），不再与平台合并退化为看全部，避免静默跨货主串数据。
	 * @return null=全部（仅平台）；否则为 IN 列表（货主=自身；服务商=名下货主；空名下/无上下文→占位 id 命中 0 行）
	 */
	public List<Long> readScope() {
		Long erp = TenantContext.getCurrentTenant();
		if (TenantContext.BLOCK_TENANT_ID.equals(erp)) {
			// 海外仓平台：看全部
			return null;
		}
		if (erp == null) {
			// 无上下文：命中 0 行（fail-closed），不退化为看全部
			return Collections.singletonList(BLOCK_ID);
		}
		Long wms = WmsTenantContext.getCurrentWmsTenant();
		if (wms != null && wms.equals(erp)) {
			// WMS 服务商：名下货主（只读）
			List<Long> ids = tenantHierarchyService.descendantErpTenantIds(wms);
			return ids.isEmpty() ? Collections.singletonList(BLOCK_ID) : ids;
		}
		// 货主：仅自己
		return Collections.singletonList(erp);
	}

	/**
	 * 解析当前货主 id：新建货主单据盖章 / 按货主隔离取数用。
	 * <p>S1 收敛：仅当存在正值货主上下文时返回该货主；无上下文或平台/服务商身份（非正值）一律 fail-closed 拒绝，
	 * 不再兜底盖成货主 1（原兜底会把漏设上下文的写入/取数静默误挂到货主 1）。
	 * @return 归属货主 id（正值）
	 * @throws TenantBusinessException 无正值货主上下文时抛出
	 */
	public Long writeOwner() {
		Long erp = TenantContext.getCurrentTenant();
		if (erp == null || erp <= 0) {
			throw new TenantBusinessException(TenantResultCode.NO_TENANT_CONTEXT);
		}
		return erp;
	}

	/**
	 * 归属校验：当前身份是否可见该单据（货主仅自己；服务商名下货主；平台放行）。
	 * @param ownerErpTenantId 单据归属货主 id
	 * @return 可见则 true
	 */
	public boolean canAccess(Long ownerErpTenantId) {
		List<Long> scope = readScope();
		return scope == null || (ownerErpTenantId != null && scope.contains(ownerErpTenantId));
	}

}
