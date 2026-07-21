package com.erp.admin.order.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 面单批次订单项 VO
 */
@Data
@Schema(title = "面单批次订单项")
public class LabelBatchItemVO {

    @Schema(title = "订单项ID")
    private Long itemId;

    @Schema(title = "订单ID")
    private Long orderId;

    @Schema(title = "平台订单号")
    private String platformOrderId;

    @Schema(title = "平台")
    private String platform;

    @Schema(title = "ERP SKU编码")
    private String erpSkuCode;

    @Schema(title = "SKU数量")
    private Integer skuQty;

    @Schema(title = "订单项状态: PENDING|SUCCESS|FAILED")
    private String status;

    @Schema(title = "失败码")
    private String failCode;

    @Schema(title = "失败原因")
    private String errorMsg;

    @Schema(title = "仓库ID")
    private String warehouseId;

    @Schema(title = "供应商ID")
    private String supplyId;

    @Schema(title = "店铺ID")
    private Long shopId;
}
