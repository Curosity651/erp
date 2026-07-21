package com.erp.admin.statistics.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售目标年度概览VO
 *
 * @author erp
 */
@Data
@Schema(title = "销售目标年度概览VO")
public class SalesTargetYearlyOverviewVO {

	@Schema(title = "年度目标")
	private SalesTargetPageVO annualTarget;

	@Schema(title = "月度目标列表")
	private List<SalesTargetPageVO> monthlyTargets;

	@Schema(title = "月度目标总和")
	private BigDecimal monthlySum;

	@Schema(title = "差额(年度目标 - 月度总和)")
	private BigDecimal difference;

	@Schema(title = "已设置的月度目标数量")
	private Integer monthlyCount;

	/**
	 * 年度实际完成金额
	 */
	@Schema(title = "年度实际完成金额")
	private BigDecimal annualActualAmount;

	/**
	 * 年度达成率(百分比)
	 */
	@Schema(title = "年度达成率(百分比)")
	private BigDecimal annualAchievementRate;

}
