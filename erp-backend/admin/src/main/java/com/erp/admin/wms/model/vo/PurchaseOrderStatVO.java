package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 采购单统计视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "采购单统计视图对象")
public class PurchaseOrderStatVO {

	@Schema(title = "采购单ID")
	private Long purchaseOrderId;

	@Schema(title = "SKU种类数")
	private Integer skuCount;

	@Schema(title = "总采购数量")
	private Integer totalQuantity;

	@Schema(title = "总已发货数量")
	private Integer totalShippedQuantity;

	@Schema(title = "总已入库数量")
	private Integer totalReceivedQuantity;

}
