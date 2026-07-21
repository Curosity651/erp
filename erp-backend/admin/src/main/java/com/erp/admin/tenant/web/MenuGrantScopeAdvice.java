package com.erp.admin.tenant.web;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.tenant.mapper.TenantRoleMapper;
import com.erp.admin.tenant.service.TenantIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 授权菜单树按身份过滤（V19 角色租户化 · 缺口A）。
 *
 * <p>BallCat 原生「角色授权」弹出的是<b>全局菜单树</b>（{@code GET /system/menu/grant-list}），货主/服务商
 * 在原生界面里本会看到并能勾选海外仓平台的菜单，这正是「问题1：货主能授平台权限」的隐患。本 Advice 在不改 BallCat
 * 代码的前提下，按当前登录身份把越权菜单从授权树里裁掉：
 * <ul>
 * <li>货主 {@code ERP_USER} 只返回货主菜单子树；</li>
 * <li>服务商 {@code WMS_OPERATOR} 只返回服务商菜单子树；</li>
 * <li>海外仓平台 {@code OVERSEAS_PLATFORM} 返回平台菜单集（用于给平台角色授权）。</li>
 * </ul>
 * 允许集由 {@link TenantRoleMapper#selectAllowedMenuIds(String)} 给出，与 {@code menu-filter.ts}、迁移 V19 一致。
 *
 * <p>实现说明：用反射读写包装类（{@code ApiResult}/{@code R}）的 {@code getData/setData} 与元素的 {@code getId}，
 * 避免硬依赖框架内部类型。仅对授权树接口生效，其余响应原样放行；身份解析异常时<b>fail-closed</b>返回空树。
 *
 * @author erp
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class MenuGrantScopeAdvice implements ResponseBodyAdvice<Object> {

	/** 仅拦截该路径的授权菜单树。 */
	private static final String GRANT_LIST_PATH = "/system/menu/grant-list";

	private final TenantIdentityService tenantIdentityService;

	private final TenantRoleMapper tenantRoleMapper;

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		// 实际判断放到 beforeBodyWrite（按 URI 精确匹配），此处统一返回 true
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
			ServerHttpResponse response) {
		if (body == null || request.getURI().getPath() == null
				|| !request.getURI().getPath().endsWith(GRANT_LIST_PATH)) {
			return body;
		}
		Object data = invoke(body, "getData");
		if (!(data instanceof List)) {
			return body;
		}
		List<?> list = (List<?>) data;
		if (list.isEmpty()) {
			return body;
		}

		Set<Long> allowed = resolveAllowedMenuIds();
		List<?> filtered = list.stream().filter(item -> allowed.contains(extractId(item))).collect(Collectors.toList());

		// 回写过滤后的列表；若包装类不支持 setData 则原样返回（不致报错，授权树保持原样）
		setData(body, filtered);
		return body;
	}

	/** 取当前身份允许的菜单 id 集合；异常时 fail-closed 返回空集（授权树为空）。 */
	private Set<Long> resolveAllowedMenuIds() {
		try {
			String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
			List<Long> ids = tenantRoleMapper.selectAllowedMenuIds(identityType);
			return ids == null ? Collections.emptySet() : new java.util.HashSet<>(ids);
		}
		catch (Exception e) {
			log.warn("授权菜单树身份过滤：解析身份失败，按空集处理", e);
			return Collections.emptySet();
		}
	}

	private static Long extractId(Object item) {
		Object id = invoke(item, "getId");
		return (id instanceof Number) ? ((Number) id).longValue() : null;
	}

	private static Object invoke(Object target, String getter) {
		if (target == null) {
			return null;
		}
		try {
			Method m = target.getClass().getMethod(getter);
			return m.invoke(target);
		}
		catch (Exception e) {
			return null;
		}
	}

	private static boolean setData(Object wrapper, Object value) {
		try {
			for (Method m : wrapper.getClass().getMethods()) {
				if ("setData".equals(m.getName()) && m.getParameterCount() == 1) {
					m.invoke(wrapper, value);
					return true;
				}
			}
		}
		catch (Exception ignore) {
			// 包装类不可写则放弃过滤回写
		}
		return false;
	}

}
