package com.erp.admin.statistics.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台履约分布VO
 *
 * @author erp
 */
@Data
@Schema(title = "平台履约分布")
public class PlatformFulfillmentVO {

	/**
	 * 平台
	 */
	@Schema(title = "平台", example = "wildberries")
	private String platform;

	/**
	 * FBS数据
	 */
	@Schema(title = "FBS数据")
	private FulfillmentDetail fbs;

	/**
	 * FBO数据
	 */
	@Schema(title = "FBO数据")
	private FulfillmentDetail fbo;

	/**
	 * 合计
	 */
	@Schema(title = "合计")
	private FulfillmentDetail total;

	/**
	 * 履约详情
	 */
	@Data
	@Schema(title = "履约详情")
	public static class FulfillmentDetail {

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

}
