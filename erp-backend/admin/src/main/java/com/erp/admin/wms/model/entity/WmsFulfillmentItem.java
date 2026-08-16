package com.erp.admin.wms.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wms_fulfillment_item")
public class WmsFulfillmentItem {
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long fulfillmentOrderId;
	private String skuCode;
	private String warehouseSkuCode;
	private String skuName;
	private String imageUrl;
	private String quality;
	private Integer quantity;
	private Integer pickedQuantity;
	private Integer outerLengthMm;
	private Integer outerWidthMm;
	private Integer outerHeightMm;
	private Integer outerGrossWeightG;
	private LocalDateTime createTime;
	private LocalDateTime updateTime;
	@TableLogic
	private Long deleted;
}
