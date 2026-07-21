package com.erp.admin.common.tenant;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.erp.admin.common.log.TenantTraceRegistry;
import com.erp.admin.tenant.mapper.ErpAccountMapper;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求级租户解析拦截器（A2 引入，B1 升级为按租户类型双层路由）。
 *
 * <p>在 Spring MVC 处理阶段（安全链之后）运行：当前登录用户 → 读 {@code sys_user.tenant_id} → 取 {@code tenant_type} →
 * 经 {@link TenantResolver} 路由，写入 {@link TenantContext}（货主维度）与 {@link WmsTenantContext}（服务商维度），
 * 请求结束清理两者。
 *
 * <p>三平台隔离：用户归属其实例（海外仓平台 / 服务商 / 货主），由 {@code sys_user.tenant_id} 单一来源解析。
 * 路由规则见 {@link TenantResolver}。
 *
 * <p>未登录 / 后台线程不进入本拦截器，自然无上下文 → {@link ErpTenantLineHandler} 方案②下不注入。
 *
 * @author erp
 */
@Component
public class TenantResolveInterceptor implements HandlerInterceptor {

	private final PrincipalAttributeAccessor principalAttributeAccessor;

	private final ErpAccountMapper erpAccountMapper;

	private final SysTenantMapper sysTenantMapper;

	public TenantResolveInterceptor(PrincipalAttributeAccessor principalAttributeAccessor,
			ErpAccountMapper erpAccountMapper, SysTenantMapper sysTenantMapper) {
		this.principalAttributeAccessor = principalAttributeAccessor;
		this.erpAccountMapper = erpAccountMapper;
		this.sysTenantMapper = sysTenantMapper;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		Long userId = currentUserId();
		if (userId == null) {
			return true;
		}
		// 三平台隔离：用户归属其实例，租户由 sys_user.tenant_id 单一来源解析（绕过租户注入读取）。
		Long tenantId = erpAccountMapper.selectTenantIdByUserId(userId);
		if (tenantId == null) {
			// 已登录但未绑定任何实例（异常/历史残留账号）：用 UNBOUND 哨兵(-999)强制 tenant_id=-999 → 业务表与
			// admin 表一律 0 行，数据级屏蔽，fail-closed。不可用 -1（那是平台真实 id，会串平台 admin 数据）。
			TenantContext.setCurrentTenant(TenantContext.UNBOUND_TENANT_ID);
			registerTrace(userId);
			return true;
		}
		// sys_tenant 不在租户白名单内，selectById 不会被自动注入 WHERE tenant_id
		SysTenant tenant = sysTenantMapper.selectById(tenantId);
		// 租户停用 → fail-closed 硬拦截：已登录会话的所有请求一律 403（菜单/数据随之失效，服务商停用级联货主）
		if (tenant != null && tenant.getStatus() != null && tenant.getStatus() != 1) {
			writeForbidden(response);
			return false;
		}
		TenantResolver.Resolution resolution = TenantResolver.resolve(tenant);
		if (resolution.getErpTenantId() != null) {
			TenantContext.setCurrentTenant(resolution.getErpTenantId());
		}
		else {
			// 类型无法识别（脏数据）：fail-closed 屏蔽，避免业务表不注入导致越权可见。同上用 UNBOUND(-999) 而非
			// -1，防止脏数据账号落进平台(-1)的 admin scope。
			TenantContext.setCurrentTenant(TenantContext.UNBOUND_TENANT_ID);
		}
		if (resolution.getWmsTenantId() != null) {
			WmsTenantContext.setCurrentWmsTenant(resolution.getWmsTenantId());
		}
		registerTrace(userId);
		return true;
	}

	/**
	 * 把当前请求的 traceId → (租户, userId, username) 登记到 {@link TenantTraceRegistry}，供脱离请求线程的访问日志
	 * 落库时回填 tenant_id/user。必须在 TenantContext 已写入后调用。任何异常都吞掉，不影响请求。
	 */
	private void registerTrace(Long userId) {
		try {
			String traceId = MDC.get("traceId");
			if (traceId == null) {
				return;
			}
			String username = null;
			try {
				username = principalAttributeAccessor.getUsername();
			}
			catch (Exception ignore) {
				// 取不到用户名不影响租户归属
			}
			TenantTraceRegistry.put(traceId, TenantContext.getCurrentTenant(), userId, username);
		}
		catch (Exception ignore) {
			// 登记失败仅使该条访问日志退回无租户，不影响主流程
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		TenantContext.clear();
		WmsTenantContext.clear();
	}

	/**
	 * 租户停用时写 403 JSON（fail-closed）。
	 */
	private static void writeForbidden(HttpServletResponse response) {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType("application/json;charset=UTF-8");
		try {
			response.getWriter().write("{\"code\":50009,\"message\":\"账号已停用，请联系管理员\"}");
		}
		catch (java.io.IOException ignore) {
			// 忽略写出异常，状态码已置 403
		}
	}

	/**
	 * 取当前登录用户 ID；未认证或异常时返回 null（视为无租户上下文）。
	 */
	private Long currentUserId() {
		try {
			return principalAttributeAccessor.getUserId();
		}
		catch (Exception ignore) {
			return null;
		}
	}

}
