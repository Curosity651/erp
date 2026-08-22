package com.erp.admin.wms.model.dto;

import java.math.BigDecimal;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "上架实际结果记录")
public class PutawayRecordDTO {

	@NotNull(message = "入库执行单ID不能为空")
	private Long inboundOrderId;

	@Valid
	@NotEmpty(message = "上架记录明细不能为空")
	private List<RecordLine> lines;

	@Schema(title = "SKU尺寸缺失时人工确认的本次总体积(m3)")
	private BigDecimal confirmedVolumeCbm;

	private Boolean afterHours;

	private String afterHoursReason;

	@Data
	public static class RecordLine {

		@NotBlank(message = "SKU编码不能为空")
		private String skuCode;

		@NotNull(message = "实际库位不能为空")
		private Long locationId;

		@NotNull(message = "上架数量不能为空")
		@Min(value = 1, message = "上架数量必须大于0")
		private Integer quantity;

		@Schema(title = "品质: GOOD良品 / DAMAGED不良品")
		private String quality;

		@Schema(title = "体积或承重超限的现场说明")
		private String capacityOverrideReason;

	}

}
