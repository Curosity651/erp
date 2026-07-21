package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 可发货采购明细查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "可发货采购明细查询对象")
public class AvailableItemQO {

	@Schema(title = "采购单号")
	private String purchaseOrderNo;

	@Schema(title = "SKU编码")
	private String skuCode;

	@Schema(title = "供应商ID")
	private Long supplierId;

	@Schema(title = "当前物流单ID（编辑时排除自身草稿占用）")
	private Long shippingOrderId;

}
