package com.erp.admin.wms.model.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class PackageScanDTO {

	@NotNull(message = "出库单ID不能为空")
	private Long outboundOrderId;

	@NotNull(message = "平台订单包裹ID不能为空")
	private Long packageId;

	@NotBlank(message = "扫描码不能为空")
	@Size(max = 256, message = "扫描码不能超过256个字符")
	private String scanCode;

	@NotNull(message = "数量不能为空")
	@Min(value = 1, message = "数量必须大于0")
	private Integer quantity;

	private Boolean manual;

	@Size(max = 200, message = "手工登记原因不能超过200个字符")
	private String manualReason;

}
