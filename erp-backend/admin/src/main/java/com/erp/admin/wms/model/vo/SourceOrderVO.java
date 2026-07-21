package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 原订单信息VO（用于退货单创建）
 *
 * @author erp
 */
@Data
@Schema(title = "原订单信息VO")
public class SourceOrderVO {

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

    @Schema(title = "仓库ID")
    private Long warehouseId;

    @Schema(title = "仓库名称")
    private String warehouseName;

    @Schema(title = "订单状态")
    private String orderStatus;

    @Schema(title = "订单时间")
    private LocalDateTime orderTime;

    @Schema(title = "可退货SKU明细")
    private List<SourceOrderItemVO> items;

    /**
     * 原订单SKU明细
     */
    @Data
    @Schema(title = "原订单SKU明细")
    public static class SourceOrderItemVO {

        @Schema(title = "SKU编码")
        private String skuCode;

        @Schema(title = "SKU名称")
        private String skuName;

        @Schema(title = "原发货数量")
        private Integer shippedQuantity;

        @Schema(title = "已退货数量")
        private Integer returnedQuantity;

        @Schema(title = "可退货数量")
        private Integer availableQuantity;

    }

}
