package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "可报废不良品批次")
public class ScrapBatchVO {

	private Long id;

	private Long sourceInventoryId;

	private String skuCode;

	private String warehouseSkuCode;

	private String locationCode;

	private String quality;

	private Integer quantity;

	private Integer reservedQty;

	private Long zoneId;

	private Long palletId;

	private Long slotId;

	private String palletNo;

	private String slotCode;

}
