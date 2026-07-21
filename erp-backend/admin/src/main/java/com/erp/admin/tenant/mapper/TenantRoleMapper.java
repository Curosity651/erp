package com.erp.admin.tenant.mapper;

import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;

/**
 * 租户角色铸造 / 菜单作用域 Mapper（V19 角色租户化）。
 *
 * <p>用途：
 * <ul>
 * <li>开通租户时铸造其<b>独立</b>管理员角色（不再共享 {@code ROLE_ADMIN}），并按身份类型授予允许的菜单集；</li>
 * <li>为「授权菜单树按身份过滤」提供某身份类型允许的菜单 id 集合。</li>
 * </ul>
 *
 * <p>{@code sys_role} 已纳入租户行级白名单（{@link com.erp.admin.common.tenant.ErpTenantLineHandler}），
 * 故 {@link #insertTenantAdminRole} 用 {@code @InterceptorIgnore} 绕过自动回填、显式写入新租户的 tenant_id，
 * 避免被回填成「开通者」的租户（如服务商开通货主时上下文为服务商）。
 *
 * <p>菜单允许集与前端 {@code menu-filter.ts} 及迁移脚本 V19 保持一致。
 *
 * @author erp
 */
public interface TenantRoleMapper {

	/**
	 * 取某身份类型允许的菜单 id 集合。
	 * @param identityType 身份类型：{@code ERP_USER} / {@code WMS_OPERATOR} / {@code OVERSEAS_PLATFORM}
	 * @return 允许的菜单 id 列表；未知类型返回空列表
	 */
	List<Long> selectAllowedMenuIds(@Param("identityType") String identityType);

	/**
	 * 铸造租户管理员角色（显式 tenant_id，绕过租户行级注入）。
	 * @param code 角色码（建议 {@code ROLE_ADMIN_T<tenantId>}）
	 * @param name 角色名
	 * @param tenantId 所属租户 id
	 * @param remarks 备注
	 * @return 影响行数
	 */
	@InterceptorIgnore(tenantLine = "true")
	int insertTenantAdminRole(@Param("code") String code, @Param("name") String name, @Param("tenantId") Long tenantId,
			@Param("remarks") String remarks);

	/**
	 * 给角色批量授菜单。
	 * @param roleCode 角色码
	 * @param menuIds 菜单 id 集合
	 * @return 影响行数
	 */
	int insertRoleMenus(@Param("roleCode") String roleCode, @Param("menuIds") List<Long> menuIds);

}
