package com.erp.admin.common.tenant;

import java.util.function.Supplier;

/**
 * 租户上下文（货主 / 第二层租户 ERP_USER）。
 *
 * <p>请求维度通过 {@link TenantFilter}（B1 引入）写入当前货主 tenant_id， MyBatis-Plus 多租户拦截器
 * {@link ErpTenantLineHandler} 据此自动注入 {@code WHERE tenant_id = ?}。 请求结束必须 {@link #clear()}。
 *
 * <p>A1 阶段拦截器白名单为空（忽略所有表），本类已就位但暂不触发注入。
 *
 * @author erp
 */
public final class TenantContext {

	/**
	 * 海外仓平台实例 id / 屏蔽哨兵（值 -1）。
	 *
	 * <p>海外仓平台是单实例，其 {@code sys_tenant.id} 即为 {@code -1}；平台用户/角色/组织等 admin 表 tenant_id 均为 -1，
	 * 据此按实例隔离。同时货主业务白名单表注入 {@code WHERE tenant_id = -1} → 永远 0 行，
	 * 在<b>数据级</b>屏蔽货主订单/商品/店铺等（平台凭身份+ {@code @InterceptorIgnore} 专用服务做物理作业）。
	 *
	 * <p>已登录但未绑定任何实例的异常账号也兜底写入 -1（fail-closed，查 0 行）。
	 */
	public static final Long BLOCK_TENANT_ID = -1L;

	/**
	 * 未绑定实例 / 脏数据账号的 fail-closed 兜底哨兵（值 -999）。
	 *
	 * <p>与 {@link #BLOCK_TENANT_ID}(-1) 的区别：-1 是海外仓平台的<b>真实</b> {@code sys_tenant.id}，其
	 * user/role/organization/project_group/position 等 admin 表 tenant_id 均为 -1（有真实数据）。若异常账号
	 * 也兜底成 -1，则会与平台 admin 数据落进同一 scope。故未绑定/脏数据账号改用一个<b>任何真实租户都不会拥有</b>的
	 * id(-999)：注入 {@code WHERE tenant_id = -999} 后，业务表与 admin 表<b>一律 0 行</b>，真正做到"什么都看不到"，
	 * 且与平台(-1)彻底分开。仅用于 {@link TenantResolveInterceptor} 的两处兜底，不参与 WMS 无货主 scope 判据。
	 */
	public static final Long UNBOUND_TENANT_ID = -999L;

	private static final ThreadLocal<Long> HOLDER = new InheritableThreadLocal<>();

	/**
	 * 「平台级跨租户扫描」放行标志（C2 fail-closed 配套）。
	 *
	 * <p>C2 收紧后：白名单表在无租户上下文时注入 {@code WHERE tenant_id = -999}（0 行，fail-closed），
	 * 不再"看全部"。但少数合法的后台平台级扫描（如同步任务先拉「全部租户的启用店铺 / 待执行财务任务」）
	 * 本就需要跨租户读取。这类调用用 {@link #runAsPlatformScan} 显式置位本标志，
	 * {@link ErpTenantLineHandler#ignoreTable} 检测到即对白名单表放行（不注入），扫描完自动复位。
	 * 未置位的无上下文线程一律 fail-closed（0 行），故遗漏放行只会"少读"而非"越权多读"。
	 */
	private static final ThreadLocal<Boolean> PLATFORM_SCAN = new ThreadLocal<>();

	private TenantContext() {
	}

	/**
	 * 设置当前货主租户
	 * @param tenantId 货主 tenant_id
	 */
	public static void setCurrentTenant(Long tenantId) {
		HOLDER.set(tenantId);
	}

	/**
	 * 获取当前货主租户
	 * @return 货主 tenant_id，无上下文时为 null
	 */
	public static Long getCurrentTenant() {
		return HOLDER.get();
	}

	/**
	 * 清理当前线程的租户上下文（请求结束必须调用，防止线程复用串租户）
	 */
	public static void clear() {
		HOLDER.remove();
	}

	/**
	 * 临时切换租户执行，结束后恢复原值。用于 WMS 回调等跨租户场景。
	 * @param tenantId 临时租户
	 * @param action 执行体
	 * @param <T> 返回类型
	 * @return 执行结果
	 */
	public static <T> T runAs(Long tenantId, Supplier<T> action) {
		Long previous = getCurrentTenant();
		try {
			setCurrentTenant(tenantId);
			return action.get();
		}
		finally {
			if (previous == null) {
				clear();
			}
			else {
				setCurrentTenant(previous);
			}
		}
	}

	/**
	 * 当前线程是否处于「平台级跨租户扫描」放行态（{@link ErpTenantLineHandler#ignoreTable} 用）。
	 * @return true=放行白名单表租户注入
	 */
	public static boolean isPlatformScan() {
		return Boolean.TRUE.equals(PLATFORM_SCAN.get());
	}

	/**
	 * 以「平台级跨租户扫描」放行态执行合法的后台跨租户读取（C2 配套），结束后自动复位。
	 * <p>仅用于确需读取全部租户数据的平台级后台查询（同步任务拉全量启用店铺 / 待执行财务任务等）。
	 * @param action 执行体
	 * @param <T> 返回类型
	 * @return 执行结果
	 */
	public static <T> T runAsPlatformScan(Supplier<T> action) {
		Boolean previous = PLATFORM_SCAN.get();
		try {
			PLATFORM_SCAN.set(Boolean.TRUE);
			return action.get();
		}
		finally {
			if (previous == null) {
				PLATFORM_SCAN.remove();
			}
			else {
				PLATFORM_SCAN.set(previous);
			}
		}
	}

}
