package com.erp.admin.wms.model.vo;

import lombok.Data;

@Data
public class LocationSlotLevelSummaryVO {

	private Long locationId;

	private Integer levelNo;

	private Integer totalCount;

	private Integer occupiedCount;

	private Integer blocked;

}
