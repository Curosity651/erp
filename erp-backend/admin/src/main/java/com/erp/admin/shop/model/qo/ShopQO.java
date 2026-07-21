package com.erp.admin.shop.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;

/**
 * 店铺（含凭证信息） 查询对象
 *
 * @author erp 2025-09-21 21:11:47
 */
@Data
@Schema(title = "店铺（含凭证信息）查询对象")
@ParameterObject
public class ShopQO {

	@Schema(title = "平台: wildberries|ozon")
	private String platform;

	@Schema(title = "状态：0=禁用 1=正常 2=异常")
	private Integer status;

	@Schema(title = "关键字（店铺名/平台店铺ID 模糊匹配）")
	private String keyword;

}