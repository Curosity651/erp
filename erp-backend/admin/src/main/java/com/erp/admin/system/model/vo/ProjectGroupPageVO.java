package com.erp.admin.system.model.vo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 项目组管理表分页视图对象
 *
 * @author ballcat 2025-07-26 15:27:25
 */
@Data
@Schema(title = "项目组管理表分页视图对象")
public class ProjectGroupPageVO {

	/**
	 * 项目组ID
	 */
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
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 更新时间
	 */
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
