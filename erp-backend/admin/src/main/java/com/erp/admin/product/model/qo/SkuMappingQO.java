package com.erp.admin.product.model.qo;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;

/**
 * SKU映射 查询对象
 *
 * @author erp 2025-09-22 21:32:36
 */
@Data
@Schema(title = "SKU映射查询对象")
@ParameterObject
public class SkuMappingQO  {

	/** 平台商品ID （精确匹配） */
	@Parameter(description="平台商品ID（精确）")
	private String platformItemId;

	/** ERP SKU编码 （精确匹配） */
	@Parameter(description="ERP SKU编码（精确）")
	private String skuCode;

}