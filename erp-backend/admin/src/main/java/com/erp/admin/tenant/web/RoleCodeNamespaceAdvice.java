package com.erp.admin.tenant.web;

import java.lang.reflect.Type;

import com.erp.admin.common.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.business.system.model.entity.SysRole;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

/**
 * 角色创建 code 自动命名空间（多租户铁律）。
 *
 * <p>BallCat 新建角色走 {@code POST /system/role} → {@code SysRoleController.save(SysRole)}，请求体为 {@link SysRole} 实体；
 * 编辑走 {@code update(SysRoleUpdateDTO)}（不同类型，不受影响）。本 Advice 仅当请求体类型为 {@link SysRole} 时生效，
 * 把租户管理员填写的 {@code code} 自动改写为按租户命名空间的全局唯一值 {@code ROLE_T<租户id>_<原值>}，
 * 使各租户角色码天然不撞、{@code sys_role_menu} 跨租户不冲突；管理员只需填显示名（name）。
 *
 * <p>幂等：已带本租户前缀的 code 不重复加。无租户上下文（异常）时不改写。
 *
 * @author erp
 */
@Slf4j
@RestControllerAdvice
public class RoleCodeNamespaceAdvice implements RequestBodyAdvice {

	@Override
	public boolean supports(MethodParameter methodParameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {
		// 仅新建角色（请求体为 SysRole 实体）命中
		return SysRole.class.equals(methodParameter.getParameterType());
	}

	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {
		return inputMessage;
	}

	@Override
	public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {
		if (body instanceof SysRole) {
			SysRole role = (SysRole) body;
			Long tenantId = currentTenant();
			if (tenantId != null && StringUtils.hasText(role.getCode())) {
				String prefix = "ROLE_T" + tenantId + "_";
				if (!role.getCode().startsWith(prefix)) {
					role.setCode(prefix + role.getCode());
				}
			}
		}
		return body;
	}

	@Override
	public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
		return body;
	}

	private Long currentTenant() {
		try {
			return TenantContext.getCurrentTenant();
		}
		catch (Exception ignore) {
			return null;
		}
	}

}
