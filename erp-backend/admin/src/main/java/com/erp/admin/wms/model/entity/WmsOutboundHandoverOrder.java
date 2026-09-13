package com.erp.admin.wms.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wms_outbound_handover_order")
public class WmsOutboundHandoverOrder {
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long tenantId;
	private String handoverNo;
	private Long fulfillmentOrderId;
	private String handoverStatus;
	private String vehiclePlate;
	private String driverName;
	private String driverPhone;
	private LocalDateTime departureTime;
	private String destination;
	private BigDecimal freightCost;
	private String currency;
	private String logisticsPhotoFileIds;
	private String remark;
	private LocalDateTime handoverTime;
	private Long handoverBy;
	private Integer version;
	private Long createBy;
	private LocalDateTime createTime;
	private Long updateBy;
	private LocalDateTime updateTime;
	@TableLogic
	private Long deleted;
}
