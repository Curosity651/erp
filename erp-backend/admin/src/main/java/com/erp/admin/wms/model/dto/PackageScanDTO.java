package com.erp.admin.wms.model.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PackageScanDTO {

	@NotNull(message = "出库单ID不能为空")
	private Long outboundOrderId;

	@NotNull(message = "平台订单包裹ID不能为空")
	private Long packageId;

	@NotBlank(message = "扫描码不能为空")
	private String scanCode;

	@NotNull(message = "数量不能为空")
	@Min(value = 1, message = "数量必须大于0")
	private Integer quantity;

	private Boolean manual;

}

