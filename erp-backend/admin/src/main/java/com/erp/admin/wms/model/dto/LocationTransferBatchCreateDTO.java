package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Schema(title = "按库位批量新建调整单DTO")
public class LocationTransferBatchCreateDTO {

	@NotNull(message = "仓库不能为空")
	private Long warehouseId;

	@NotBlank(message = "调整原因不能为空")
	private String reasonCode;

	private String reason;

	private String remark;

	@Valid
	@NotEmpty(message = "调整明细不能为空")
	private List<LocationTransferItemDTO> items;

}
