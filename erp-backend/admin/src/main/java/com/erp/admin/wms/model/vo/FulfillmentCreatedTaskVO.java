package com.erp.admin.wms.model.vo;

import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import lombok.Getter;

@Getter
public class FulfillmentCreatedTaskVO {
	private final Long taskId;
	private final String taskNo;
	private final Long warehouseId;
	private final Integer orderCount;
	private final Integer totalQuantity;

	public FulfillmentCreatedTaskVO(WmsFulfillmentPickTask task) {
		this.taskId = task.getId();
		this.taskNo = task.getTaskNo();
		this.warehouseId = task.getWarehouseId();
		this.orderCount = task.getOrderCount();
		this.totalQuantity = task.getTotalQuantity();
	}
}
