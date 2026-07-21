package com.erp.admin.statistics.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 目标进度VO
 *
 * @author erp
 */
@Data
@Schema(title = "目标进度")
public class TargetProgressVO {

	/**
	 * 月度目标
	 */
	@Schema(title = "月度目标")
	private TargetDetailVO monthly;

	/**
	 * 年度目标
	 */
	@Schema(title = "年度目标")
	private TargetDetailVO yearly;

	/**
	 * 目标详情
	 */
	@Data
	@Schema(title = "目标详情")
	public static class TargetDetailVO {

		/**
		 * 目标金额
		 */
		@Schema(title = "目标金额")
		private BigDecimal target;

		/**
		 * 当前金额
		 */
		@Schema(title = "当前金额")
		private BigDecimal current;

		/**
		 * 完成百分比
		 */
		@Schema(title = "完成百分比", description = "0-100的数值")
		private BigDecimal progress;

		/**
		 * 是否设置了目标
		 */
		@Schema(title = "是否设置了目标")
		private Boolean hasTarget;

	}

}
