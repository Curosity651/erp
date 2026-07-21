package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 库位调整（库内移库）DTO：把源批次的部分/全部数量移到同仓的目标库位。
 *
 * @author erp
 */
@Data
@Schema(title = "库位调整DTO")
public class LocationTransferDTO {

	@NotNull(message = "仓库不能为空")
	@Schema(title = "仓库ID")
	private Long warehouseId;

	@NotNull(message = "源批次不能为空")
	@Schema(title = "源批次ID（wms_physical_inventory.id）")
	private Long physicalInventoryId;

	@NotNull(message = "移动数量不能为空")
	@Min(value = 1, message = "移动数量必须大于0")
	@Schema(title = "移动数量")
	private Integer quantity;

	@NotBlank(message = "目标库位不能为空")
	@Size(max = 50, message = "目标库位编码长度不能超过50")
	@Schema(title = "目标库位编码")
	private String targetLocationCode;

}
