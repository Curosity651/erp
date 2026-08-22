package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PackShipPackagePageVO {

	private Long id;
	private Long outboundOrderId;
	private String outboundNo;
	private Long erpOrderId;
	private String platformOrderId;
	private String platform;
	private Long shopId;
	private String shopName;
	private Long erpTenantId;
	private String ownerName;
	private Long operatorId;
	private String operatorName;
	private String warehouseName;
	private Long sortSlotId;
	private String sortSlotScanCode;
	private String sortCode;
	private Integer skuKinds;
	private Integer totalQty;
	private String labelStatus;
	private String packStatus;
	private String shipStatus;
	private String workStatus;
	private String documentMode;
	private String defaultChannelCode;
	private String channelCode;
	private String channelName;
	private String trackingNo;
	private BigDecimal weight;
	private String packerName;
	private String shippedByName;
	private String createTime;

}
