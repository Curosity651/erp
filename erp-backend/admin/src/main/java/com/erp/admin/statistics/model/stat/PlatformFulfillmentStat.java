package com.erp.admin.statistics.model.stat;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台履约统计结果
 *
 * @author erp
 */
@Data
public class PlatformFulfillmentStat {

	/**
	 * 平台
	 */
	private String platform;

	/**
	 * 履约类型
	 */
	private String fulfillmentType;

	/**
	 * 订单数量
	 */
	private Long orderCount;

	/**
	 * 总金额
	 */
	private BigDecimal totalAmount;

}
