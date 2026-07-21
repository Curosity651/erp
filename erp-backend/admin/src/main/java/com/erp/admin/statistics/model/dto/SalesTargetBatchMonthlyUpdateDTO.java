package com.erp.admin.statistics.model.dto;

import com.erp.admin.system.model.dto.AnnualTargetDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 销售目标批量月度更新DTO
 *
 * @author erp
 */
@Data
@Schema(title = "销售目标批量月度更新DTO")
public class SalesTargetBatchMonthlyUpdateDTO {

	@NotNull(message = "年份不能为空")
	@Schema(title = "目标年份")
	private Integer year;

	@Schema(title = "货币单位")
	private String currency;

	@Schema(title = "年度目标ID")
	private Long annualTargetId;

	@Valid
	@Schema(title = "年度目标")
	private AnnualTargetDTO annualTarget;

	@Valid
	@Schema(title = "更新目标列表")
	private List<SalesTargetUpdateDTO> updateTargets;

	@Valid
	@Schema(title = "创建目标列表")
	private List<MonthlyTargetDTO> createTargets;

}
