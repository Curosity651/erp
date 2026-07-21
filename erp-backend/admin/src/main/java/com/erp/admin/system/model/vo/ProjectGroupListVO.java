package com.erp.admin.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 项目组列表VO（用于下拉选择）
 *
 * @author ballcat
 */
@Data
@Schema(title = "项目组列表VO")
public class ProjectGroupListVO {

	@Schema(title = "项目组ID")
	private Long id;

	@Schema(title = "项目组名称")
	private String name;

	@Schema(title = "项目组编码")
	private String code;

	@Schema(title = "状态")
	private Integer status;

}
