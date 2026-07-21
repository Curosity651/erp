package com.erp.admin.tenant.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.erp.admin.tenant.model.entity.SysUserAccount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

/**
 * 系统用户账号 Mapper（开通租户建管理员账号 + 租户解析读用户归属）。
 *
 * <p>命名避开 BallCat 自带的 {@code SysUserMapper}（同名 bean 冲突）。
 *
 * <p>注意：{@code sys_user} 已纳入租户行级白名单（三平台用户隔离），因此「全局唯一性校验」「按用户读其归属租户」
 * 这类**跨实例**查询必须用 {@code @InterceptorIgnore} 绕过租户注入，否则会被当前实例上下文误过滤。
 *
 * @author erp
 */
public interface ErpAccountMapper extends ExtendMapper<SysUserAccount> {

	/**
	 * 用户名是否已存在（<b>全局</b>，跨所有实例）。登录按用户名全局解析用户，故用户名必须全局唯一。
	 * @param username 用户名
	 * @return 存在则 true
	 */
	default boolean existsByUsername(String username) {
		return countByUsernameGlobal(username) > 0;
	}

	/**
	 * 全局统计某用户名的有效用户数（绕过租户注入）。
	 * @param username 用户名
	 * @return 数量
	 */
	@InterceptorIgnore(tenantLine = "true")
	@Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username} AND deleted = 0")
	long countByUsernameGlobal(@Param("username") String username);

	/**
	 * 读取用户所属的租户/实例 id（绕过租户注入；租户解析阶段尚无上下文）。
	 * @param userId 用户 id
	 * @return tenant_id；未设置时为 null
	 */
	@InterceptorIgnore(tenantLine = "true")
	@Select("SELECT tenant_id FROM sys_user WHERE user_id = #{userId} AND deleted = 0")
	Long selectTenantIdByUserId(@Param("userId") Long userId);

	/**
	 * 按用户 id 取用户名（全局，绕过租户注入）。用于日志操作人归属（用唯一 user_id 解析，避免重名歧义）。
	 * @param userId 用户 id
	 * @return 用户名；不存在时 null
	 */
	@InterceptorIgnore(tenantLine = "true")
	@Select("SELECT username FROM sys_user WHERE user_id = #{userId} AND deleted = 0")
	String selectUsernameById(@Param("userId") Long userId);

	/**
	 * 按用户名取所属租户/实例 id（全局，绕过租户注入）。用户名全局唯一（uk_username_deleted），故结果唯一。
	 * 用于登录日志按租户归属：登录事件仅有 username，需据此反解 tenant_id 再写库。
	 * @param username 用户名
	 * @return tenant_id；不存在或未设置时 null
	 */
	@InterceptorIgnore(tenantLine = "true")
	@Select("SELECT tenant_id FROM sys_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
	Long selectTenantIdByUsername(@Param("username") String username);

	/**
	 * 读取用户的实例管理员标记（绕过租户注入）。
	 * @param userId 用户 id
	 * @return is_admin（1=管理员）；不存在时 null
	 */
	@InterceptorIgnore(tenantLine = "true")
	@Select("SELECT is_admin FROM sys_user WHERE user_id = #{userId} AND deleted = 0")
	Integer selectIsAdminById(@Param("userId") Long userId);

}
