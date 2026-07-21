package com.erp.admin.statistics.model.vo;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SKU排名项VO
 *
 * @author erp
 */
@Data
@Schema(title = "SKU排名项")
public class SkuRankingItemVO {

	/**
	 * 排名
	 */
	@Schema(title = "排名")
	private Integer rank;

	/**
	 * SKU编码或Article
	 */
	@Schema(title = "SKU编码", description = "如果已映射则为SKU编码，否则为Article")
	private String sku;

	/**
	 * 是否已映射
	 */
	@Schema(title = "是否已映射", description = "true表示已在sku_mapping表中映射")
	private Boolean isMapped;

	/**
	 * 销量
	 */
	@Schema(title = "销量")
	private Long quantity;

	/**
	 * 销售金额（卢布）
	 */
	@Schema(title = "销售金额", description = "单位: 卢布")
	private BigDecimal amount;

	/**
	 * SKU ID
	 */
	@Schema(title = "SKU ID", description = "已映射时才有值")
	private Long skuId;

	/**
	 * 产品中文名称
	 */
	@Schema(title = "产品中文名称", description = "已映射时才有值")
	private String skuNameCn;

	/**
	 * 产品主图URL
	 */
	@Schema(title = "产品主图URL", description = "已映射时才有值")
	private String imageUrl;

	/**
	 * 品类ID
	 */
	@Schema(title = "品类ID", description = "已映射时才有值")
	private Long categoryId;

	/**
	 * 品类名称
	 */
	@Schema(title = "品类名称", description = "已映射时才有值")
	private String categoryName;

}
