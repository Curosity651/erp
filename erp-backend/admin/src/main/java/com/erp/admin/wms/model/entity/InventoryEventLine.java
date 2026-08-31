package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wms_inventory_event_line")
public class InventoryEventLine {
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long eventId;
	private Integer lineNo;
	private Long inventoryId;
	private Long locationId;
	private Long counterpartLocationId;
	private String skuCode;
	private String quality;
	private Integer quantityDelta;
	private Integer reservedDelta;
	private Integer beforeQuantity;
	private Integer afterQuantity;
	private Integer beforeReserved;
	private Integer afterReserved;
	private LocalDateTime createTime;
}
