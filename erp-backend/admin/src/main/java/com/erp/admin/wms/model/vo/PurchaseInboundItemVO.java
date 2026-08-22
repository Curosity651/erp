package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 采购入库单明细视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "采购入库单明细视图对象")
public class PurchaseInboundItemVO {

    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "物流单明细ID")
    private Long shippingOrderItemId;

    @Schema(title = "采购单ID")
    private Long purchaseOrderId;

    @Schema(title = "采购单号")
    private String purchaseOrderNo;

    @Schema(title = "采购单明细ID")
    private Long purchaseOrderItemId;

    @Schema(title = "SKU编码")
    private String skuCode;

	@Schema(title = "海外仓内部SKU编码")
	private String warehouseSkuCode;

	@Schema(title = "SKU简要信息")
	private SkuBriefVO skuBrief;

    @Schema(title = "应到数量")
    private Integer expectedQuantity;

    @Schema(title = "实到数量")
    private Integer actualQuantity;

    @Schema(title = "货品预判(仅自定义退货单): GOOD良品 / PENDING待检 / DAMAGED不良品")
    private String expectedQuality;

    @Schema(title = "未到数量（应到 - 实到，始终为正数）")
    private Integer shortQuantity;

    @Schema(title = "备注")
    private String remark;

}
