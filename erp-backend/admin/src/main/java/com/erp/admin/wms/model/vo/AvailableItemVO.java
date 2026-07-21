package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 可发货采购明细视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "可发货采购明细视图对象")
public class AvailableItemVO {

	@Schema(title = "采购单ID")
	private Long purchaseOrderId;

	@Schema(title = "采购单号")
	private String purchaseOrderNo;

	@Schema(title = "采购单明细ID")
	private Long purchaseOrderItemId;

	@Schema(title = "SKU编码")
	private String skuCode;

	@Schema(title = "SKU简要信息")
	private SkuBriefVO skuBrief;

	@Schema(title = "采购数量")
	private Integer purchaseQuantity;

	@Schema(title = "已发货数量")
	private Integer shippedQuantity;

	@Schema(title = "可发货数量")
	private Integer availableQuantity;

	@Schema(title = "供应商ID")
	private Long supplierId;

	@Schema(title = "供应商名称")
	private String supplierName;

}
