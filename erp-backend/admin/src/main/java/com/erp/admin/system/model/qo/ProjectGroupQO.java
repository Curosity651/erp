package com.erp.admin.system.model.qo;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;

/**
 * 项目组管理表 查询对象
 *
 * @author ballcat 2025-07-26 15:27:25
 */
@Data
@Schema(title = "项目组管理表查询对象")
@ParameterObject
public class ProjectGroupQO {

	/**
	 * 项目组名称
	 */
	@Parameter(description = "项目组名称")
	private String name;

	/**
	 * 项目组编码
	 */
	@Parameter(description = "项目组编码")
	private String code;

	/**
	 * 状态（1-启用，0-停用）
	 */
	@Parameter(description = "状态（1-启用，0-停用）")
	private Integer status;

}
