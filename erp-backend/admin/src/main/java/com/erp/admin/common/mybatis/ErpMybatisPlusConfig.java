package com.erp.admin.common.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.erp.admin.common.tenant.ErpTenantLineHandler;
import com.erp.admin.wms.datascope.WmsOwnerDataPermissionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class ErpMybatisPlusConfig {
    // WmsOwnerDataPermissionHandler 以 @Lazy 注入：它间接依赖 SysTenantMapper，
    // 而 Mapper 依赖 sqlSessionFactory（本拦截器是其构建入参），直接注入会形成构建期循环依赖。
    // @Lazy 注入代理，首次查询时才真正初始化（此时 sqlSessionFactory 已就绪），打破环。
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(@Lazy WmsOwnerDataPermissionHandler wmsOwnerDataPermissionHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 多租户拦截器必须位于分页拦截器之前。Tier T(含日志/文件)按 tenant_id 自动注入；wms_* 由下方数据权限按 erp_tenant_id 处理。
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new ErpTenantLineHandler()));
        // 数据可见性：货主单据「按 erp_tenant_id 隔离」（日志/文件已改由上面的租户行级拦截器按 tenant_id 处理）。
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(wmsOwnerDataPermissionHandler));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
		interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

		return interceptor;
    }
}
