package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 新建库位调整单 DTO（平台）。
 *
 * @author erp
 */
@Data
@Schema(title = "新建库位调整单DTO")
public class LocationTransferCreateDTO {

	@NotNull(message = "仓库不能为空")
	@Schema(title = "仓库ID")
	private Long warehouseId;

	@NotNull(message = "货主不能为空")
	@Schema(title = "货主ID")
	private Long erpTenantId;

	@Schema(title = "备注")
	private String remark;

	@Valid
	@NotEmpty(message = "调整明细不能为空")
	@Schema(title = "调整明细")
	private List<LocationTransferItemDTO> items;

}
