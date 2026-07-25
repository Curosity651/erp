package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Schema(title = "完善库位调整计划")
public class LocationTransferPlanDTO {

	@Valid
	@NotEmpty(message = "计划明细不能为空")
	private List<LocationTransferPlanItemDTO> items;

}
