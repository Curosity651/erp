package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 可退货订单VO（item 级别，每行一个可退货 item）
 *
 * @author system
 */
@Data
@Schema(title = "可退货订单VO（item 级别）")
public class ReturnableOrderVO {

    @Schema(title = "订单ID")
    private Long orderId;

    @Schema(title = "平台订单号")
    private String platformOrderId;

    @Schema(title = "平台")
    private String platform;

    @Schema(title = "发货时间")
    private LocalDateTime outboundTime;

    // ===== item 级别字段 =====

    @Schema(title = "订单商品明细ID (erp_order_item.id)")
    private Long orderItemId;

    @Schema(title = "平台商品ID")
    private String platformItemId;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "SKU摘要信息")
    private SkuBriefVO skuBrief;

    @Schema(title = "发货数量")
    private Integer shippedQuantity;

    @Schema(title = "已退货数量")
    private Integer returnedQuantity;

    @Schema(title = "处理中退货数量")
    private Integer inFlightQuantity;

    @Schema(title = "可退货数量")
    private Integer returnableQuantity;
}
