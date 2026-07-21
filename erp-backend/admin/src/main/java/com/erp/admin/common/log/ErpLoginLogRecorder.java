package com.erp.admin.common.log;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.tenant.mapper.ErpAccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.business.log.enums.LoginEventTypeEnum;
import org.ballcat.business.log.handler.LoginLogUtils;
import org.ballcat.business.log.model.entity.LoginLog;
import org.ballcat.business.log.service.LoginLogService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 登录日志记录器（登录日志「零记录」修复 + 按租户回填）。
 *
 * <p>问题：BallCat 只在 <b>OAuth2 授权服务器</b>登录流下注册 {@code SpringAuthorizationServerLoginLogHandler}
 * （{@code @ConditionalOnBean(AuthorizationServerSettings)}）。本项目走 <b>form-login</b>，条件不成立 →
 * 没有任何登录日志 handler → {@code log_login_log} 一条不写。
 *
 * <p>修复：直接监听 Spring Security 认证事件（成功/失败/登出）记录登录日志。{@code LoginLog} 实体只有 {@code username}
 * （无 userId/tenantId），故按 username 反解 tenant_id，用 {@link TenantContext#runAs} 在保存那一刻重建上下文 →
 * 租户拦截器在 INSERT 自动追加 {@code tenant_id} → 各租户只看自己的登录日志。用户名未知/查不到租户（如失败登录时用户名
 * 不存在）则 tenant_id 留 NULL，不误挂到任何租户。记录逻辑全程 try/catch，绝不影响认证主流程。
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErpLoginLogRecorder {

	/** 登录状态：BallCat 约定 1=成功、0=失败。 */
	private static final int STATUS_SUCCESS = 1;

	private static final int STATUS_FAIL = 0;

	/** msg 列长度上限保护。 */
	private static final int MSG_MAX = 200;

	private final LoginLogService loginLogService;

	private final ErpAccountMapper erpAccountMapper;

	/** 登录成功。 */
	@EventListener
	public void onSuccess(AuthenticationSuccessEvent event) {
		record(nameOf(event.getAuthentication()), STATUS_SUCCESS, LoginEventTypeEnum.LOGIN, "登录成功");
	}

	/** 登录失败（用户名/密码错误、账号停用等）。 */
	@EventListener
	public void onFailure(AbstractAuthenticationFailureEvent event) {
		String msg = event.getException() != null ? event.getException().getMessage() : "登录失败";
		record(nameOf(event.getAuthentication()), STATUS_FAIL, LoginEventTypeEnum.LOGIN, msg);
	}

	/** 登出成功。 */
	@EventListener
	public void onLogout(LogoutSuccessEvent event) {
		record(nameOf(event.getAuthentication()), STATUS_SUCCESS, LoginEventTypeEnum.LOGOUT, "登出成功");
	}

	private static String nameOf(Authentication authentication) {
		return authentication != null ? authentication.getName() : null;
	}

	/**
	 * 组装并按租户保存登录日志。任何异常都吞掉，不影响认证。
	 */
	private void record(String username, int status, LoginEventTypeEnum eventType, String msg) {
		try {
			if (!StringUtils.hasText(username)) {
				return;
			}
			LoginLog loginLog = LoginLogUtils.prodLoginLog(username);
			loginLog.setStatus(status);
			loginLog.setEventType(eventType.getValue());
			loginLog.setMsg(truncate(msg));

			Long tenantId = resolveTenant(username);
			if (tenantId == null) {
				loginLogService.save(loginLog);
			}
			else {
				TenantContext.runAs(tenantId, () -> loginLogService.save(loginLog));
			}
		}
		catch (Exception e) {
			log.warn("记录登录日志失败(username={}, eventType={}): {}", username, eventType, e.getMessage());
		}
	}

	/** 按用户名反解 tenant_id（绕过租户注入；用户名全局唯一）。 */
	private Long resolveTenant(String username) {
		try {
			return erpAccountMapper.selectTenantIdByUsername(username);
		}
		catch (Exception ignore) {
			return null;
		}
	}

	private static String truncate(String s) {
		if (s == null) {
			return null;
		}
		return s.length() <= MSG_MAX ? s : s.substring(0, MSG_MAX);
	}

}
