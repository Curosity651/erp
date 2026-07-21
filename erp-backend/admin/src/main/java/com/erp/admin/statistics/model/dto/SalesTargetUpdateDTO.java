package com.erp.admin.statistics.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 销售目标更新DTO
 *
 * @author erp
 */
@Data
@Schema(title = "销售目标更新DTO")
public class SalesTargetUpdateDTO {

	@NotNull(message = "ID不能为空")
	@Schema(title = "目标ID")
	private Long id;

	@NotNull(message = "目标金额不能为空")
	@DecimalMin(value = "0.01", message = "目标金额必须大于0")
	@Digits(integer = 18, fraction = 2, message = "目标金额最多18位整数和2位小数")
	@Schema(title = "目标金额")
	private BigDecimal targetAmount;

	@Size(max = 255, message = "备注长度不能超过255个字符")
	@Schema(title = "备注")
	private String remark;

}
