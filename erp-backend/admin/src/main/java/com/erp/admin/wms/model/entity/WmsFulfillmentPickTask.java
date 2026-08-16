package com.erp.admin.wms.model.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wms_fulfillment_pick_task")
public class WmsFulfillmentPickTask {
	@TableId(type = IdType.AUTO)
	private Long id;
	private String taskNo;
	private Long warehouseId;
	private Long wmsTenantId;
	private Long erpTenantId;
	private String taskStatus;
	private Integer orderCount;
	private Integer totalQuantity;
	private Long operatorId;
	private LocalDateTime createTime;
	private LocalDateTime updateTime;
}
