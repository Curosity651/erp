package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购单明细视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "采购单明细视图对象")
public class PurchaseOrderItemVO {

	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "采购单ID")
	private Long purchaseOrderId;

	@Schema(title = "SKU编码")
	private String skuCode;

	@Schema(title = "采购数量")
	private Integer quantity;

	@Schema(title = "单价")
	private BigDecimal unitPrice;

	@Schema(title = "金额")
	private BigDecimal amount;

	@Schema(title = "已发货数量")
	private Integer shippedQuantity;

	@Schema(title = "已入库数量")
	private Integer receivedQuantity;

	@Schema(title = "备注")
	private String remark;

}
