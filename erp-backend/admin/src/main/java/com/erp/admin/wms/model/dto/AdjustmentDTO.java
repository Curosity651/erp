package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 报废单数据传输对象（平台发起报废）。
 *
 * @author erp
 */
@Data
@Schema(title = "报废单数据传输对象")
public class AdjustmentDTO {

	@NotNull(message = "调整仓库不能为空")
	@Schema(title = "调整仓库ID")
	private Long warehouseId;

	@NotNull(message = "货主不能为空")
	@Schema(title = "货主（货物归属），整单归属一个货主")
	private Long erpTenantId;

	@Schema(title = "调整类型：恒为 SCRAP-报废")
	private String adjustmentType;

	@Schema(title = "调整日期（不传取当天）")
	private LocalDate adjustmentDate;

	@Size(max = 500, message = "调整原因长度不能超过500")
	@Schema(title = "调整原因")
	private String adjustmentReason;

	@Valid
	@NotEmpty(message = "报废明细不能为空")
	@Schema(title = "报废明细列表（每行锁定一个批次）")
	private List<AdjustmentItemDTO> items;

}
