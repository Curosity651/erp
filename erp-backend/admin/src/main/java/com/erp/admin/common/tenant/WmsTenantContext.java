package com.erp.admin.common.tenant;

import java.util.function.Supplier;

/**
 * WMS 服务商上下文（第一层租户 WMS_OPERATOR）。
 *
 * <p>WMS 服务商用户登录后，{@link TenantFilter}（B1 引入）将其 tenant_id 写入本上下文； WMS 物理表查询据此按
 * {@code wms_tenant_id} 隔离（WMS Mapper 用 {@code @InterceptorIgnore} 绕过 ERP 自动注入， 显式按本上下文过滤）。
 *
 * @author erp
 */
public final class WmsTenantContext {

	private static final ThreadLocal<Long> HOLDER = new InheritableThreadLocal<>();

	private WmsTenantContext() {
	}

	/**
	 * 设置当前 WMS 服务商租户
	 * @param wmsTenantId WMS 服务商 tenant_id
	 */
	public static void setCurrentWmsTenant(Long wmsTenantId) {
		HOLDER.set(wmsTenantId);
	}

	/**
	 * 获取当前 WMS 服务商租户
	 * @return WMS 服务商 tenant_id，无上下文时为 null
	 */
	public static Long getCurrentWmsTenant() {
		return HOLDER.get();
	}

	/**
	 * 清理当前线程的 WMS 租户上下文（请求结束必须调用）
	 */
	public static void clear() {
		HOLDER.remove();
	}

	/**
	 * 临时切换 WMS 服务商执行，结束后恢复原值
	 * @param wmsTenantId 临时 WMS 服务商
	 * @param action 执行体
	 * @param <T> 返回类型
	 * @return 执行结果
	 */
	public static <T> T runAs(Long wmsTenantId, Supplier<T> action) {
		Long previous = getCurrentWmsTenant();
		try {
			setCurrentWmsTenant(wmsTenantId);
			return action.get();
		}
		finally {
			if (previous == null) {
				clear();
			}
			else {
				setCurrentWmsTenant(previous);
			}
		}
	}

}
