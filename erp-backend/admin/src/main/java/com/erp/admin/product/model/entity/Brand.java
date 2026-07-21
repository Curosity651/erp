package com.erp.admin.product.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 品牌管理
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@Data
@TableName("brand")
@Schema(title = "品牌管理")
public class Brand {

	private static final long serialVersionUID = 1L;

	/**
	 * 品牌ID
	 */
	@TableId
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
	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 更新时间
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
