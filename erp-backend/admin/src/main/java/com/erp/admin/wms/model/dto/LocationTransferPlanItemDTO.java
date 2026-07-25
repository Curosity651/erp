package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(title = "完善库位调整计划明细")
public class LocationTransferPlanItemDTO {

	@NotNull(message = "调整明细ID不能为空")
	private Long id;

	@NotBlank(message = "目标库位不能为空")
	private String targetLocationCode;

}
