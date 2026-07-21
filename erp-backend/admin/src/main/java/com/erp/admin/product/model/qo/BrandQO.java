package com.erp.admin.product.model.qo;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;

/**
 * 品牌管理 查询对象
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@Data
@Schema(title = "品牌管理查询对象")
@ParameterObject
public class BrandQO {

	/**
	 * 品牌名称
	 */
	@Parameter(description = "品牌名称")
	private String name;

	/**
	 * 品牌编码
	 */
	@Parameter(description = "品牌编码")
	private String code;

	/**
	 * 品牌原产国家/地区
	 */
	@Parameter(description = "品牌原产国家/地区")
	private String originCountry;

	/**
	 * 状态（1-启用，0-停用）
	 */
	@Parameter(description = "状态（1-启用，0-停用）")
	private Integer status;

}
