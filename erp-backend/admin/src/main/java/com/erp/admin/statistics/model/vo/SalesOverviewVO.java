package com.erp.admin.statistics.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 销售概览VO
 *
 * @author erp
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "销售概览")
public class SalesOverviewVO {

	/**
	 * 总销售额（卢布）
	 */
	@Schema(title = "总销售额", description = "查询时间范围内的总销售额，单位: 卢布")
	private BigDecimal totalSales;

}
