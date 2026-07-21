package com.erp.admin.product.model.vo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 品牌管理分页视图对象
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@Data
@Schema(title = "品牌管理分页视图对象")
public class BrandPageVO {

	private static final long serialVersionUID = 1L;

	/**
	 * 品牌ID
	 */
	@Schema(title = "品牌ID")
	private Long id;

	/**
	 * 品牌名称
	 */
	@Schema(title = "品牌名称")
	private String name;

	/**
	 * 品牌编码，唯一
	 */
	@Schema(title = "品牌编码，唯一")
	private String code;

	/**
	 * 品牌LOGO URL
	 */
	@Schema(title = "品牌LOGO URL")
	private String logoUrl;

	/**
	 * 品牌原产国家/地区
	 */
	@Schema(title = "品牌原产国家/地区")
	private String originCountry;

	/**
	 * 品牌介绍
	 */
	@Schema(title = "品牌介绍")
	private String description;

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
