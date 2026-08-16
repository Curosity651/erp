package com.erp.admin.wms.model.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wms_fulfillment_pick_task_order")
public class WmsFulfillmentPickTaskOrder {
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long taskId;
	private Long fulfillmentOrderId;
	private Integer sequenceNo;
	private String orderStatus;
	private LocalDateTime createTime;
	private LocalDateTime updateTime;
}
