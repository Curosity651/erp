package com.erp.admin.statistics.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 汇率信息VO
 *
 * @author erp
 */
@Data
@Schema(title = "汇率信息")
public class ExchangeRatesVO {

	/**
	 * 汇率日期
	 */
	@Schema(title = "汇率日期")
	private LocalDate date;

	/**
	 * 汇率列表
	 */
	@Schema(title = "汇率列表")
	private List<RateItem> rates;

	/**
	 * 汇率项
	 */
	@Data
	@Schema(title = "汇率项")
	public static class RateItem {

		/**
		 * 货币代码
		 */
		@Schema(title = "货币代码", example = "USD")
		private String currency;

		/**
		 * 汇率
		 */
		@Schema(title = "汇率", description = "1单位该货币兑换CNY的汇率")
		private BigDecimal rate;

	}

}
