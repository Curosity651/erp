package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wms_stocktake_location_task")
public class StocktakeLocationTask {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long stocktakeOrderId;

	private Long warehouseId;

	private Long zoneId;

	private Long locationId;

	private String locationCode;

	private String taskStatus;

	private Long assigneeId;

	private String assigneeName;

	private LocalDateTime startedTime;

	private LocalDateTime completedTime;

	private Integer locationConfirmed;

	@Version
	private Integer version;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateTime;
}

