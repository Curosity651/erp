package com.erp.admin.order.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 月度金额DTO
 *
 * @author erp
 */
@Data
public class MonthlyAmountDTO {

	/**
	 * 月份 (1-12)
	 */
	private Integer month;

	/**
	 * 金额
	 */
	private BigDecimal amount;

}
