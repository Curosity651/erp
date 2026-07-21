package com.erp.admin.common.tenant;

import com.erp.admin.tenant.enums.TenantType;
import com.erp.admin.tenant.model.entity.SysTenant;

/**
 * 租户上下文解析（纯函数，B1 引入）。
 *
 * <p>输入登录用户所属的 {@link SysTenant}，输出「应写入哪两个上下文」，<b>不</b>直接操作 ThreadLocal，
 * 以便单元测试。实际写入由 {@link TenantResolveInterceptor} 完成。
 *
 * <p>路由规则（三平台隔离）：
 * <table border="1">
 * <caption>tenant_type → 上下文</caption>
 * <tr><th>租户类型</th><th>erpTenantId（→ {@link TenantContext}）</th><th>wmsTenantId（→ {@link WmsTenantContext}）</th></tr>
 * <tr><td>ERP_USER（货主）</td><td>自身 id</td><td>parent_wms_tenant_id（所属服务商）</td></tr>
 * <tr><td>WMS_OPERATOR（服务商）</td><td>自身 id（无 OMS 数据 → 货主 OMS 表查得 0 行）</td><td>自身 id（看名下所有货主物理库存）</td></tr>
 * <tr><td>OVERSEAS_PLATFORM（海外仓平台，单实例）</td><td>自身 id（业务表查 0 行）</td><td>null（物理作业走 @InterceptorIgnore + 身份校验）</td></tr>
 * </table>
 *
 * <p>三类登录用户都会得到 erpTenantId，因此 OMS/ERP 业务表永远被过滤；无上下文仅剩后台线程（方案②原意）。
 *
 * @author erp
 */
public final class TenantResolver {

	private TenantResolver() {
	}

	/**
	 * 解析租户应写入的上下文。
	 * @param tenant 登录用户所属租户；为 null 或类型无法识别时返回 {@link Resolution#NONE}
	 * @return 解析结果
	 */
	public static Resolution resolve(SysTenant tenant) {
		if (tenant == null) {
			return Resolution.NONE;
		}
		TenantType type = TenantType.of(tenant.getTenantType());
		if (type == null) {
			return Resolution.NONE;
		}
		switch (type) {
			case ERP_USER:
				// 货主：OMS/ERP 表按自己过滤；WMS 表按其所属平台过滤
				return new Resolution(tenant.getId(), tenant.getParentWmsTenantId());
			case WMS_OPERATOR:
				// WMS 服务商：OMS/ERP 表按自己（无业务数据→屏蔽货主 OMS）；WMS 表按自己（看名下所有货主物理库存）
				return new Resolution(tenant.getId(), tenant.getId());
			case OVERSEAS_PLATFORM:
				// 海外仓平台（单实例）：admin/系统表按平台自身实例 scope；业务表按平台 id 查得 0 行（平台无 OMS 数据）。
				// 物理作业（收货/上架/库存）走 @InterceptorIgnore 专用服务 + 身份校验，不依赖 WmsTenantContext，故此处不写 WMS 上下文。
				return new Resolution(tenant.getId(), null);
			default:
				return Resolution.NONE;
		}
	}

	/**
	 * 解析结果：货主上下文 + WMS 平台上下文。任一可能为 null（表示该上下文不写入）。
	 */
	public static final class Resolution {

		/** 空解析：两个上下文都不写入（如未绑定租户、类型未知）。 */
		public static final Resolution NONE = new Resolution(null, null);

		private final Long erpTenantId;

		private final Long wmsTenantId;

		public Resolution(Long erpTenantId, Long wmsTenantId) {
			this.erpTenantId = erpTenantId;
			this.wmsTenantId = wmsTenantId;
		}

		/** 写入 {@link TenantContext} 的货主 tenant_id；null 表示不写。 */
		public Long getErpTenantId() {
			return this.erpTenantId;
		}

		/** 写入 {@link WmsTenantContext} 的平台 tenant_id；null 表示不写。 */
		public Long getWmsTenantId() {
			return this.wmsTenantId;
		}

		/** 两个上下文都为空。 */
		public boolean isEmpty() {
			return this.erpTenantId == null && this.wmsTenantId == null;
		}

	}

}
