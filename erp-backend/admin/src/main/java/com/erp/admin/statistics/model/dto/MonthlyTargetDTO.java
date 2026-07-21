package com.erp.admin.statistics.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 月度目标DTO
 *
 * @author erp
 */
@Data
@Schema(title = "月度目标DTO")
public class MonthlyTargetDTO {

	@Schema(title = "目标年份")
	private Integer year;

	@NotNull(message = "月份不能为空")
	@Min(value = 1, message = "月份必须在1-12之间")
	@Max(value = 12, message = "月份必须在1-12之间")
	@Schema(title = "月份", description = "1-12")
	private Integer month;

	@DecimalMin(value = "0.01", message = "目标金额必须大于0")
	@Digits(integer = 18, fraction = 2, message = "目标金额最多18位整数和2位小数")
	@Schema(title = "目标金额")
	private BigDecimal amount;

	@Size(max = 255, message = "备注长度不能超过255个字符")
	@Schema(title = "备注")
	private String remark;

}
