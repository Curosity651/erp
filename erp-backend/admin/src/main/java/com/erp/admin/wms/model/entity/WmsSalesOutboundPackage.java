package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

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
	private String sortCode;
	private String sortStatus;
	private String sortBy;
	private LocalDateTime sortTime;
	private String labelStatus;
	private Integer handoverRequired;
	private String handoverStatus;
	private String packStatus;
	private String packerName;
	private LocalDateTime packTime;
	@Version
	private Integer version;
	private LocalDateTime createTime;
	private LocalDateTime updateTime;

}
