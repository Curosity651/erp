package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wms_outbound_scan_event")
public class WmsOutboundScanEvent {

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long taskId;
	private Long taskLineId;
	private Long packageId;
	private String stage;
	private String eventType;
	private String scanCode;
	private String skuCode;
	private Integer quantity;
	private Long operatorId;
	private String operatorName;
	private String remark;
	private LocalDateTime createTime;

}

