package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wms_sales_outbound_package")
public class WmsSalesOutboundPackage {

	@TableId
	private Long id;
	private Long outboundOrderId;
	private Long erpOrderId;
	private Long erpTenantId;
	private String platform;
	private Long shopId;
	private String platformOrderId;
	private Long sortSlotId;
	private String sortSlotScanCode;
	private String sortCode;
	private String sortStatus;
	private Long sortById;
	private String sortBy;
	private LocalDateTime sortTime;
	private String labelStatus;
	private Integer handoverRequired;
	private String handoverStatus;
	private String packStatus;
	private Long packerId;
	private String packerName;
	private LocalDateTime packTime;
	private String shipStatus;
	private String channelCode;
	private String channelName;
	private String trackingNo;
	private BigDecimal weight;
	private Long shippedBy;
	private String shippedByName;
	private LocalDateTime shippedTime;
	@Version
	private Integer version;
	private LocalDateTime createTime;
	private LocalDateTime updateTime;

}
