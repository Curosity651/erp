package com.erp.admin.statistics.model.stat;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售趋势统计结果
 *
 * @author erp
 */
@Data
public class SalesTrendStat {

	/**
	 * 时间点
	 */
	private String timePoint;

	/**
	 * 总销售额
	 */
	private BigDecimal totalSales;

	/**
	 * 有效销售额
	 */
	private BigDecimal effectiveSales;

}
