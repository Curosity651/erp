package com.erp.admin.wms.model.vo;

import java.time.LocalDate;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 物流单待入库明细视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "物流单待入库明细视图对象")
public class ShippingItemForInboundVO {

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

	@Schema(title = "SKU简要信息")
	private SkuBriefVO skuBrief;

	@Schema(title = "发货数量")
    private Integer shippedQuantity;

    @Schema(title = "已入库数量")
    private Integer receivedQuantity;

    @Schema(title = "待入库数量（发货数量 - 已入库数量）")
    private Integer pendingQuantity;

    @Schema(title = "采购单预计交货日期（用于排序）")
    private LocalDate expectedDeliveryDate;

}
