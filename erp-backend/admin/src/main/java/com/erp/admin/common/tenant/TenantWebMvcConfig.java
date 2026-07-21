package com.erp.admin.common.tenant;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册请求级租户解析拦截器（A2 引入）。
 *
 * @author erp
 */
@Configuration
public class TenantWebMvcConfig implements WebMvcConfigurer {

	private final TenantResolveInterceptor tenantResolveInterceptor;

	public TenantWebMvcConfig(TenantResolveInterceptor tenantResolveInterceptor) {
		this.tenantResolveInterceptor = tenantResolveInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(this.tenantResolveInterceptor).addPathPatterns("/**");
	}

}
