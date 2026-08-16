package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wms_putaway_receipt_line")
public class WmsPutawayReceiptLine {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long inboundOrderId;

	private Long locationId;

	private String locationCode;

	private Long palletId;

	private String palletNo;

	private String slotCode;

	private String skuCode;

	private String quality;

	private Integer quantity;

	private String overrideReason;

	private LocalDateTime createTime;

}
