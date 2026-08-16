package com.erp.admin.wms.model.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class LogicalTransferLocationVO {

	private Long locationId;
	private String locationCode;
	private String rackNo;
	private Integer columnNo;
	private String locationType;
	private String zoneName;
	private String zoneType;
	private Integer publicShared;
	private BigDecimal utilizationPercent;

}
