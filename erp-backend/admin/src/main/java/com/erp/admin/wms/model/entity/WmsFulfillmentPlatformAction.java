package com.erp.admin.wms.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wms_fulfillment_platform_action")
public class WmsFulfillmentPlatformAction {
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long fulfillmentOrderId;
	private String actionType;
	private String actionStatus;
	private String requestFingerprint;
	private String requestPayload;
	private String responsePayload;
	private String errorMessage;
	private Integer attemptCount;
	private LocalDateTime startedTime;
	private LocalDateTime completedTime;
	private LocalDateTime createTime;
	private LocalDateTime updateTime;
}
