package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wms_inventory_event")
public class InventoryEvent {
	@TableId(type = IdType.AUTO)
	private Long id;
	private String eventNo;
	private Long tenantId;
	private Long wmsTenantId;
	private Long erpTenantId;
	private Long warehouseId;
	private String eventType;
	private String sourceType;
	private Long sourceId;
	private String sourceNo;
	private Long operatorId;
	private String operatorName;
	private String reason;
	private String idempotencyKey;
	private LocalDateTime occurredAt;
	private LocalDateTime createTime;
}
