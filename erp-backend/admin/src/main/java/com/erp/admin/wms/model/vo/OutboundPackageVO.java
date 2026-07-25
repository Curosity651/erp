package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class OutboundPackageVO {

	private Long id;
	private Long outboundOrderId;
	private Long erpOrderId;
	private String platformOrderId;
	private Long shopId;
	private String sortCode;
	private String sortStatus;
	private String labelStatus;
	private Boolean handoverRequired;
	private String handoverStatus;
	private String packStatus;
	private List<PackShipItemVO> items;

}
