package com.erp.admin.wms.model.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(title = "修改逻辑库位")
public class LogicalLocationUpdateDTO {

	@NotNull(message = "库位分区不能为空")
	private Long zoneId;

	@NotBlank(message = "库位类型不能为空")
	private String locationType;

	@NotNull(message = "库位长度不能为空")
	@Min(value = 1, message = "库位长度必须大于0")
	private Integer lengthMm;

	@NotNull(message = "库位宽度不能为空")
	@Min(value = 1, message = "库位宽度必须大于0")
	private Integer widthMm;

	@NotNull(message = "库位高度不能为空")
	@Min(value = 1, message = "库位高度必须大于0")
	private Integer heightMm;

	@NotNull(message = "最大承重不能为空")
	@DecimalMin(value = "0.01", message = "最大承重必须大于0")
	private BigDecimal maxWeightKg;

	@NotNull(message = "最大SKU种类数不能为空")
	@Min(value = 0, message = "最大SKU种类数不能小于0")
	private Integer maxSkuKinds;

	@NotNull(message = "请选择是否公共共享")
	@Min(value = 0, message = "公共共享标记不正确")
	@Max(value = 1, message = "公共共享标记不正确")
	private Integer publicShared;

}
