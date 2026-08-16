package com.erp.admin.wms.model.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wms_fulfillment_pick_task_line")
public class WmsFulfillmentPickTaskLine {
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long taskId;
	private Long fulfillmentOrderId;
	private Long reservationId;
	private Long inventoryId;
	private Long locationId;
	private String locationCode;
	private Long fulfillmentItemId;
	private String skuCode;
	private String warehouseSkuCode;
	private Integer sequenceNo;
	private Integer plannedQuantity;
	private Integer pickedQuantity;
	private Integer returnedQuantity;
	private String lineStatus;
	private Integer version;
	private LocalDateTime createTime;
	private LocalDateTime updateTime;
}
