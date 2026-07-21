package com.erp.admin.product.model.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SKU文件类型配置表
 *
 * @author ballcat 2025-08-02
 */
@Data
@TableName("sku_file_type_config")
@Schema(title = "SKU文件类型配置表")
public class SkuFileTypeConfig implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键ID
	 */
	@TableId
	@Schema(title = "主键ID")
	private Long id;

	/**
	 * 文件分类：image=图片，document=工程文件
	 */
	@NotBlank(message = "文件分类不能为空")
	@Size(max = 20, message = "文件分类长度不能超过20个字符")
	@Schema(title = "文件分类", description = "image=图片，document=工程文件")
	private String fileCategory;

	/**
	 * 文件类型代码
	 */
	@NotBlank(message = "文件类型代码不能为空")
	@Size(max = 50, message = "文件类型代码长度不能超过50个字符")
	@Schema(title = "文件类型代码")
	private String fileType;

	/**
	 * 文件类型名称
	 */
	@NotBlank(message = "文件类型名称不能为空")
	@Size(max = 100, message = "文件类型名称长度不能超过100个字符")
	@Schema(title = "文件类型名称")
	private String typeName;

	/**
	 * 文件类型描述
	 */
	@Size(max = 255, message = "文件类型描述长度不能超过255个字符")
	@Schema(title = "文件类型描述")
	private String description;

	/**
	 * 是否必传（1=是，0=否）
	 */
	@NotNull(message = "是否必传不能为空")
	@Schema(title = "是否必传", description = "1=是，0=否")
	private Integer isRequired;

	/**
	 * 最少文件数量
	 */
	@Schema(title = "最少文件数量")
	private Integer minCount;

	/**
	 * 最多文件数量
	 */
	@Schema(title = "最多文件数量")
	private Integer maxCount;

	/**
	 * 允许的文件扩展名，逗号分隔
	 */
	@Size(max = 500, message = "允许的文件扩展名长度不能超过500个字符")
	@Schema(title = "允许的文件扩展名", description = "逗号分隔")
	private String allowedExtensions;

	/**
	 * 单个文件最大大小(字节)
	 */
	@Schema(title = "单个文件最大大小(字节)")
	private Long maxFileSize;

	/**
	 * 显示排序
	 */
	@Schema(title = "显示排序")
	private Integer sortOrder;

	/**
	 * 状态（1=启用，0=停用）
	 */
	@Schema(title = "状态", description = "1=启用，0=停用")
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
