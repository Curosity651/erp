package com.erp.admin.product.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SKU选择弹窗查询对象 - 轻量级，仅包含选择弹窗搜索所需字段
 *
 * @author ballcat
 */
@Data
@Schema(title = "SKU选择查询对象")
public class SkuSelectQO {

	/**
	 * SKU编码（模糊查询）
	 */
	@Schema(title = "SKU编码")
	private String skuCode;

	/**
	 * SKU名称（中文名或俄文名，模糊查询）
	 */
	@Schema(title = "SKU名称")
	private String skuName;

	/**
	 * 所属货主ID：仅平台端跨货主选品时传，服务端按其 tenant_id 临时切上下文查该货主 SKU 目录；
	 * 非平台身份忽略此值（防越权）。
	 */
	@Schema(title = "所属货主ID（平台端跨货主选品用）")
	private Long erpTenantId;

}
