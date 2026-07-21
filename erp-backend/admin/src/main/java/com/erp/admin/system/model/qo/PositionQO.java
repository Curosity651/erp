package com.erp.admin.system.model.qo;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;

/**
 * 岗位管理表 查询对象
 *
 * @author ballcat 2025-07-26 15:27:26
 */
@Data
@Schema(title = "岗位管理表查询对象")
@ParameterObject
public class PositionQO {

	@Parameter(description = "岗位名称")
	private String name;

	@Parameter(description = "岗位编码")
	private String code;

}
