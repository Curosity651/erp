package com.erp.admin.wms.model.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class ShipPackageDTO {

	@NotNull(message = "出库单ID不能为空")
	private Long outboundOrderId;

	@NotNull(message = "平台订单包裹ID不能为空")
	private Long packageId;

	@NotBlank(message = "物流渠道不能为空")
	@Size(max = 64, message = "物流渠道不能超过64个字符")
	private String channel;

	@Size(max = 128, message = "跟踪号不能超过128个字符")
	private String trackingNo;

	@NotNull(message = "重量不能为空")
	@DecimalMin(value = "0.01", message = "重量必须大于0")
	private BigDecimal weight;

}
