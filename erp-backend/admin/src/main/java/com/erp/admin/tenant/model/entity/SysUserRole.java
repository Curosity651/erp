package com.erp.admin.tenant.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户-角色关联（只读用：身份解析读取当前用户角色码）。
 *
 * <p>表结构由 BallCat 提供：{@code (id, user_id, role_code)}，无逻辑删除列。
 *
 * @author erp
 */
@Data
@TableName("sys_user_role")
@Schema(title = "用户角色关联")
public class SysUserRole {

	@TableId(type = IdType.AUTO)
	private Long id;

	@Schema(title = "用户ID")
	private Long userId;

	@Schema(title = "角色编码")
	private String roleCode;

}
