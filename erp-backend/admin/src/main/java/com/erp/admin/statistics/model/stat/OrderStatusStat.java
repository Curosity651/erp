package com.erp.admin.statistics.model.stat;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单状态统计结果
 *
 * @author erp
 */
@Data
public class OrderStatusStat {

	/**
	 * 订单状态
	 */
	private String status;

	/**
	 * 订单数量
	 */
	private Long orderCount;

	/**
	 * 总金额
	 */
	private BigDecimal totalAmount;

}
