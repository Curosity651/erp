package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wms_sort_slot")
public class WmsSortSlot {

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long warehouseId;
	private String slotCode;
	private String scanCode;
	private String slotStatus;
	private Long taskId;
	private Long outboundOrderId;
	private Long packageId;
	private LocalDateTime reservedTime;
	private LocalDateTime occupiedTime;
	private LocalDateTime readyTime;
	private LocalDateTime packingTime;
	private LocalDateTime releasedTime;
	private Integer version;
	private LocalDateTime createTime;
	private LocalDateTime updateTime;

}
