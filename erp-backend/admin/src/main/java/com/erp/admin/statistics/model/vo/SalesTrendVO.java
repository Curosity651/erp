package com.erp.admin.statistics.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售趋势VO
 *
 * @author erp
 */
@Data
@Schema(title = "销售趋势")
public class SalesTrendVO {

	/**
	 * 时间粒度
	 */
	@Schema(title = "时间粒度", description = "hour | day")
	private String granularity;

	/**
	 * 趋势数据
	 */
	@Schema(title = "趋势数据")
	private List<TrendPoint> data;

	/**
	 * 趋势点
	 */
	@Data
	@Schema(title = "趋势点")
	public static class TrendPoint {

		/**
		 * 时间点
		 */
		@Schema(title = "时间点", description = "格式: YYYY-MM-DD HH:mm 或 YYYY-MM-DD")
		private String time;

		/**
		 * 总销售额（卢布）
		 */
		@Schema(title = "总销售额", description = "所有状态订单的销售额")
		private BigDecimal totalSales;

		/**
		 * 有效销售额（卢布）
		 */
		@Schema(title = "有效销售额", description = "已签收订单的销售额")
		private BigDecimal effectiveSales;

	}

}
