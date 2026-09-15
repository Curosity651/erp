package com.erp.admin.product.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SKU选择弹窗视图对象 - 轻量级，仅包含选择弹窗展示所需字段
 *
 * @author ballcat
 */
@Data
@Schema(title = "SKU选择视图对象")
public class SkuSelectVO {

	/**
	 * 主键ID
	 */
	@Schema(title = "主键ID")
	private Long id;

	/**
	 * SKU编码
	 */
	@Schema(title = "SKU编码")
	private String skuCode;

	/**
	 * 中文名
	 */
	@Schema(title = "中文名")
	private String chineseName;

	/**
	 * 主图URL
	 */
	@Schema(title = "主图URL")
	private String mainImage;

	/**
	 * 品类全路径名称
	 */
	@Schema(title = "品类全路径名称")
	private String categoryFullPath;

	/** 外箱长度（毫米） */
	private Integer outerLengthMm;

	/** 外箱宽度（毫米） */
	private Integer outerWidthMm;

	/** 外箱高度（毫米） */
	private Integer outerHeightMm;

}
