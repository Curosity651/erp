package com.erp.admin.wms.model.dto;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "仓库托盘规则")
public class WarehousePalletRuleDTO {

	@NotNull(message = "仓库ID不能为空")
	private Long id;

	@NotNull(message = "单托最多品种数不能为空")
	@Min(value = 1, message = "单托最多品种数不能小于1")
	@Max(value = 4, message = "单托最多品种数不能超过4")
	private Integer maxSkuKindsPerPallet;

	@NotNull(message = "跨货主混托设置不能为空")
	@Min(value = 0, message = "跨货主混托设置不正确")
	@Max(value = 1, message = "跨货主混托设置不正确")
	private Integer allowCrossOwnerMix;

	@NotNull(message = "托盘长度不能为空")
	@Min(value = 1, message = "托盘长度必须大于0")
	private Integer defaultPalletLengthMm;

	@NotNull(message = "托盘宽度不能为空")
	@Min(value = 1, message = "托盘宽度必须大于0")
	private Integer defaultPalletWidthMm;

	@NotNull(message = "托盘高度不能为空")
	@Min(value = 1, message = "托盘高度必须大于0")
	private Integer defaultPalletHeightMm;

	@NotNull(message = "托盘承重不能为空")
	@DecimalMin(value = "0.01", message = "托盘承重必须大于0")
	private BigDecimal defaultPalletMaxWeightKg;

	@NotNull(message = "托盘利用率不能为空")
	@DecimalMin(value = "0.10", message = "托盘利用率不能小于0.10")
	@DecimalMax(value = "1.00", message = "托盘利用率不能大于1.00")
	private BigDecimal defaultPalletUtilization;

}
