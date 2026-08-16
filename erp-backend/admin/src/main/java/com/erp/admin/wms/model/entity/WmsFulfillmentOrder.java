package com.erp.admin.wms.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import lombok.Data;

@Data
@TableName("wms_fulfillment_order")
public class WmsFulfillmentOrder {
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long tenantId;
	private Long wmsTenantId;
	private Long erpTenantId;
	private Long warehouseId;
	private Long shopId;
	private String fulfillmentNo;
	private String sourceType;
	private Long sourceOrderId;
	private String sourceOrderNo;
	private String platformStatus;
	private FulfillmentStatus fulfillmentStatus;
	private String recipientName;
	private String recipientPhone;
	private String recipientAddress;
	private String carrierCode;
	private String carrierName;
	private String trackingNo;
	private BigDecimal packageWeightKg;
	private String labelFileUrl;
	private String labelBarcode;
	private LocalDateTime labelFetchedTime;
	private LocalDateTime labelVerifiedTime;
	private LocalDateTime packedTime;
	private LocalDateTime shippedTime;
	private String cancelReason;
	private Integer version;
	private Long createBy;
	private LocalDateTime createTime;
	private Long updateBy;
	private LocalDateTime updateTime;
	@TableLogic
	private Long deleted;
}
