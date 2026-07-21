package com.erp.admin.statistics.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单状态分布VO
 *
 * @author erp
 */
@Data
@Schema(title = "订单状态分布")
public class OrderStatusVO {

	/**
	 * 订单状态
	 */
	@Schema(title = "订单状态", description = "ErpOrderStatusEnum枚举值")
	private String status;

	/**
	 * 订单数量
	 */
	@Schema(title = "订单数量")
	private Long count;

	/**
	 * 销售金额（卢布）
	 */
	@Schema(title = "销售金额", description = "单位: 卢布")
	private BigDecimal amount;

}
