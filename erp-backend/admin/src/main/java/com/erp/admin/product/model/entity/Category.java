package com.erp.admin.product.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商品品类
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@Data
@TableName("category")
@Schema(title = "商品品类")
public class Category {

	private static final long serialVersionUID = 1L;

	/**
	 * 品类ID
	 */
	@TableId
	@Schema(title = "品类ID")
	private Long id;

	/**
	 * 品类名称
	 */
	@Schema(title = "品类名称")
	private String name;

	/**
	 * 品类编码，唯一
	 */
	@Schema(title = "品类编码，唯一")
	private String code;

	/**
	 * 上级品类ID，NULL为顶级
	 */
	@Schema(title = "上级品类ID，NULL为顶级")
	private Long parentId;

	/**
	 * 品类层级（1级/2级/3级等）
	 */
	@Schema(title = "品类层级（1级/2级/3级等）")
	private Integer level;

	/**
	 * 排序
	 */
	@Schema(title = "排序")
	private Integer sort;

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
