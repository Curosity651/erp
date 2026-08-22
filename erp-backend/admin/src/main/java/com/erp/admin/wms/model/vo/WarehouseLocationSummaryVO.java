package com.erp.admin.wms.model.vo;

import lombok.Data;

/** Actual logical-location counts for one warehouse. */
@Data
public class WarehouseLocationSummaryVO {

	private Long warehouseId;

	private Integer actualLocationCount;

	private Integer actualRackCount;

	private Integer assignableRackCount;

}
