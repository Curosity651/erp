package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 物流单明细视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "物流单明细视图对象")
public class ShippingOrderItemVO {

	@Schema(title = "主键ID")
	private Long id;

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

	@Schema(title = "发货数量")
	private Integer quantity;

	@Schema(title = "当前物流单可使用的发货数量")
	private Integer availableQuantity;

	@Schema(title = "已到货数量")
	private Integer receivedQuantity;

	@Schema(title = "备注")
	private String remark;

}
