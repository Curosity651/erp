package com.erp.admin.system.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 项目组管理表
 *
 * @author ballcat 2025-07-26 15:27:25
 */
@Data
@TableName("project_group")
@Schema(title = "项目组管理表")
public class ProjectGroup {

	/**
	 * 项目组ID
	 */
	@TableId
	@Schema(title = "项目组ID")
	private Long id;

	/**
	 * 项目组名称
	 */
	@Schema(title = "项目组名称")
	private String name;

	/**
	 * 项目组编码，唯一
	 */
	@Schema(title = "项目组编码，唯一")
	private String code;

	/**
	 * 项目组描述
	 */
	@Schema(title = "项目组描述")
	private String description;

	/**
	 * 状态（1-启用，0-停用）
	 */
	@Schema(title = "状态（1-启用，0-停用）")
	private Integer status;

	/**
	 * 创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 更新时间
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
