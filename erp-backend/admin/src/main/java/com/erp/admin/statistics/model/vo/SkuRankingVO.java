package com.erp.admin.statistics.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU排名VO
 *
 * @author erp
 */
@Data
@Schema(title = "SKU排名")
public class SkuRankingVO {

	/**
	 * TOP5热销SKU
	 */
	@Schema(title = "TOP5热销SKU")
	private List<SkuItem> top5;

	/**
	 * BOTTOM5滞销SKU
	 */
	@Schema(title = "BOTTOM5滞销SKU")
	private List<SkuItem> bottom5;

	/**
	 * SKU项
	 */
	@Data
	@Schema(title = "SKU项")
	public static class SkuItem {

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

	}

}
