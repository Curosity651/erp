package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * 库位调整明细 DTO（一行 = 一个源批次移到一个目标库位）。
 *
 * @author erp
 */
@Data
@Schema(title = "库位调整明细DTO")
public class LocationTransferItemDTO {

	@NotNull(message = "源批次不能为空")
	@Schema(title = "源批次ID（wms_physical_inventory.id）")
	private Long physicalInventoryId;

	@NotNull(message = "移动数量不能为空")
	@Positive(message = "移动数量必须为正数")
	@Schema(title = "移动数量")
	private Integer quantity;

	@NotBlank(message = "目标库位不能为空")
	@Schema(title = "目标库位编码")
	private String targetLocationCode;

	@Schema(title = "备注")
	private String remark;

}
