package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StocktakeLocationTaskVO {

	private Long id;
	private Long stocktakeOrderId;
	private Long warehouseId;
	private Long zoneId;
	private Long locationId;
	private String locationCode;
	private String taskStatus;
	private String assigneeName;
	private Integer itemCount;
	private Integer countedCount;
	private Integer diffCount;
	private LocalDateTime completedTime;
}

