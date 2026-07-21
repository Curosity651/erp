package com.erp.admin.tenant.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 系统用户账号（精简：仅供开通租户时新建管理员账号使用）。
 *
 * <p>映射 BallCat 的 {@code sys_user} 表，但只声明开通所需字段；其余列（create_time/deleted 等）走数据库默认值。
 * 类名/Mapper 名避开 BallCat 自带 {@code SysUser}/{@code SysUserMapper} 的 bean 冲突。
 *
 * @author erp
 */
@Data
@TableName("sys_user")
@Schema(title = "系统用户账号")
public class SysUserAccount {

	@TableId(type = IdType.AUTO)
	@Schema(title = "用户ID")
	private Long userId;

	@Schema(title = "用户名")
	private String username;

	@Schema(title = "昵称")
	private String nickname;

	@Schema(title = "密码（已加密）")
	private String password;

	@Schema(title = "盐值")
	private String salt;

	@Schema(title = "状态：1启用/0停用")
	private Integer status;

	@Schema(title = "类型")
	private Integer type;

	@Schema(title = "组织ID")
	private Integer organizationId;

	@Schema(title = "所属租户/实例 id（三平台隔离：用户归属其实例）")
	private Long tenantId;

	@Schema(title = "实例管理员标记 1是/0否（替代 sys_user_tenant.is_admin）")
	private Integer isAdmin;

}
