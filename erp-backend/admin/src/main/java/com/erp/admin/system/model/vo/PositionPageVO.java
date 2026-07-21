package com.erp.admin.system.model.vo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 岗位管理表分页视图对象
 *
 * @author ballcat 2025-07-26 15:27:26
 */
@Data
@Schema(title = "岗位管理表分页视图对象")
public class PositionPageVO {

	/**
	 * 岗位ID
	 */
	@Schema(title = "岗位ID")
	private Long id;

	/**
	 * 岗位名称
	 */
	@Schema(title = "岗位名称")
	private String name;

	/**
	 * 岗位编码，唯一
	 */
	@Schema(title = "岗位编码，唯一")
	private String code;

	/**
	 * 岗位描述
	 */
	@Schema(title = "岗位描述")
	private String description;

	/**
	 * 显示顺序
	 */
	@Schema(title = "显示顺序")
	private Integer sort;

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
