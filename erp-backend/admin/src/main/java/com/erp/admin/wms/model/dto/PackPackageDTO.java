package com.erp.admin.wms.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class PackPackageDTO {

	@NotNull(message = "出库单ID不能为空")
	private Long outboundOrderId;

	@NotNull(message = "平台订单包裹ID不能为空")
	private Long packageId;

	private String packerName;

}

