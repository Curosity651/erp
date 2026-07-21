package com.erp.admin.system.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 年度目标DTO
 *
 * @author erp
 */
@Data
@Schema(title = "年度目标DTO")
public class AnnualTargetDTO {

	@NotNull(message = "年度目标金额不能为空")
	@DecimalMin(value = "0.01", message = "年度目标金额必须大于0")
	@Digits(integer = 18, fraction = 2, message = "年度目标金额最多18位整数和2位小数")
	@Schema(title = "年度目标金额")
	private BigDecimal amount;

	@Size(max = 255, message = "备注长度不能超过255个字符")
	@Schema(title = "备注")
	private String remark;

}
