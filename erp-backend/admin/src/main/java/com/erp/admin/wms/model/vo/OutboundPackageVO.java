package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OutboundPackageVO {

	private Long id;
	private Long outboundOrderId;
	private Long erpOrderId;
	private String platformOrderId;
	private Long shopId;
	private Long sortSlotId;
	private String sortSlotScanCode;
	private String sortCode;
	private String sortStatus;
	private String labelStatus;
	private Boolean handoverRequired;
	private String handoverStatus;
	private String packStatus;
	private String shipStatus;
	private String channelCode;
	private String channelName;
	private String trackingNo;
	private BigDecimal weight;
	private Long shippedBy;
	private String shippedByName;
	private LocalDateTime shippedTime;
	private List<PackShipItemVO> items;

}
