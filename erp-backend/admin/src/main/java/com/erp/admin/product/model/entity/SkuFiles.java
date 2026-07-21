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
 * SKU文件表
 *
 * @author ballcat 2025-07-27 02:02:06
 */
@Data
@TableName("sku_files")
@Schema(title = "SKU文件表")
public class SkuFiles implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键ID
	 */
	@TableId
	@Schema(title = "主键ID")
	private Long id;

	/**
	 * SKU ID
	 */
	@NotNull(message = "SKU ID不能为空")
	@Schema(title = "SKU ID")
	private Long skuId;

	/**
	 * 文件类型：product_image、platform_image、manual、box_mark等
	 */
	@NotBlank(message = "文件类型不能为空")
	@Size(max = 50, message = "文件类型长度不能超过50个字符")
	@Schema(title = "文件类型", description = "product_image、platform_image、manual、box_mark等")
	private String fileType;

	/**
	 * OSS对象键
	 */
	@NotBlank(message = "OSS对象键不能为空")
	@Size(max = 500, message = "OSS对象键长度不能超过500个字符")
	@Schema(title = "OSS对象键", description = "用于构建文件访问URL")
	private String objectKey;

	/**
	 * 排序顺序
	 */
	@Schema(title = "排序顺序", description = "用于文件显示排序，数值越小越靠前")
	private Integer sortOrder;

	/**
	 * 创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 创建人
	 */
	@Schema(title = "创建人")
	private String createBy;

}
