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
@Schema(title = "新增逻辑库位")
public class LogicalLocationCreateDTO {

	@NotNull(message = "仓库不能为空")
	private Long warehouseId;

	@NotNull(message = "库位分区不能为空")
	private Long zoneId;

	@NotBlank(message = "排号不能为空")
	private String rackNo;

	@NotNull(message = "排内顺序不能为空")
	@Min(value = 1, message = "排内顺序不能小于1")
	@Max(value = 9999, message = "排内顺序不能超过9999")
	private Integer sequenceNo;

	@NotBlank(message = "库位编号不能为空")
	private String locationCode;

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
