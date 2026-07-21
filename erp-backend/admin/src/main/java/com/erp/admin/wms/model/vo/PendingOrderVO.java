package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 待出库订单VO
 *
 * @author erp
 */
@Data
@Schema(title = "待出库订单VO")
public class PendingOrderVO {

    @Schema(title = "订单ID")
    private Long id;

    @Schema(title = "平台订单号")
    private String platformOrderId;

    @Schema(title = "平台类型")
    private String platform;

    @Schema(title = "店铺ID")
    private Long shopId;

    @Schema(title = "店铺名称")
    private String shopName;

    @Schema(title = "订单创建时间")
    private String orderTime;

    @Schema(title = "订单SKU明细")
    private List<PendingOrderItemVO> items;

    @Schema(title = "总数量")
    private Integer totalQuantity;

    @Schema(title = "可用库存数量")
    private Integer availableStock;

    @Schema(title = "库存状态：sufficient/insufficient/zero")
    private String stockStatus;

}
