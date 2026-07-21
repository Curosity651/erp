package com.erp.admin.product.model.qo;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;

/**
 * 供应商 查询对象
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@Data
@Schema(title = "供应商查询对象")
@ParameterObject
public class SupplierQO {

	/**
	 * 供应商编码
	 */
	@Parameter(description = "供应商编码")
	private String supplierCode;

	/**
	 * 供应商名称
	 */
	@Parameter(description = "供应商名称")
	private String name;

	/**
	 * 供应商所在城市
	 */
	@Parameter(description = "供应商所在城市")
	private String city;

	/**
	 * 状态（1-启用，0-停用）
	 */
	@Parameter(description = "状态（1-启用，0-停用）")
	private Integer status;

	/**
	 * 业务联系人
	 */
	@Parameter(description = "业务联系人")
	private String businessContactName;

	/**
	 * 业务联系人电话
	 */
	@Parameter(description = "业务联系人电话")
	private String businessContactPhone;

}
