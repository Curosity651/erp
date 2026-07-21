package com.erp.admin.tenant.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.erp.admin.tenant.model.entity.SysUserRole;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 用户-角色 Mapper（只读：身份解析用）。
 *
 * <p>命名避开 BallCat 自带的 {@code org.ballcat.business.system.mapper.SysUserRoleMapper}（同名 bean 冲突）。
 *
 * @author erp
 */
public interface ErpUserRoleMapper extends ExtendMapper<SysUserRole> {

	/**
	 * 查询用户的角色码列表（保留备用；身份解析已改为按 tenant_type 判定）。
	 * @param userId 用户 ID
	 * @return 角色码列表，无则空列表
	 */
	default List<String> selectRoleCodesByUserId(Long userId) {
		if (userId == null) {
			return Collections.emptyList();
		}
		LambdaQueryWrapperX<SysUserRole> wrapper = WrappersX.lambdaQueryX(SysUserRole.class)
			.select(SysUserRole::getRoleCode)
			.eq(SysUserRole::getUserId, userId);
		return this.selectList(wrapper).stream().map(SysUserRole::getRoleCode).collect(Collectors.toList());
	}

}
