package com.erp.admin.product.model.qo;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;

/**
 * 商品品类 查询对象
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@Data
@Schema(title = "商品品类查询对象")
@ParameterObject
public class CategoryQO {

	/**
	 * 品类名称
	 */
	@Parameter(description = "品类名称")
	private String name;

	/**
	 * 品类编码
	 */
	@Parameter(description = "品类编码")
	private String code;

	/**
	 * 上级品类ID
	 */
	@Parameter(description = "上级品类ID")
	private Long parentId;

	/**
	 * 状态（1-启用，0-停用）
	 */
	@Parameter(description = "状态（1-启用，0-停用）")
	private Integer status;

}
