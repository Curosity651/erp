package com.erp.admin.statistics.model.dto;

import com.erp.admin.system.model.dto.AnnualTargetDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 销售目标批量创建DTO
 *
 * @author erp
 */
@Data
@Schema(title = "销售目标批量创建DTO")
public class SalesTargetBatchDTO {

	@NotNull(message = "年份不能为空")
	@Schema(title = "目标年份")
	private Integer year;

	@NotBlank(message = "货币单位不能为空")
	@Size(min = 3, max = 3, message = "货币单位必须为3个字符")
	@Schema(title = "货币单位", description = "如: CNY, USD, EUR")
	private String currency;

	@NotNull(message = "年度目标不能为空")
	@Valid
	@Schema(title = "年度目标")
	private AnnualTargetDTO annualTarget;

	@Size(max = 12, message = "月度目标最多12个")
	@Valid
	@Schema(title = "月度目标列表")
	private List<MonthlyTargetDTO> monthlyTargets;

}
