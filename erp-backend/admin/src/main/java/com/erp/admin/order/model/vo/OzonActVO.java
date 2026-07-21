package com.erp.admin.order.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 单份 Ozon 运单（交接单 act）视图对象。
 *
 * @author system
 */
@Data
@Schema(title = "Ozon运单")
public class OzonActVO {

    @Schema(title = "本地运单ID(ozon_shipment_act.id)")
    private Long actId;

    private Long shopId;

    private String shopName;

    private Long deliveryMethodId;

    private String deliveryMethodName;

    private String warehouseName;

    @Schema(title = "发货日期 yyyy-MM-dd")
    private String departureDate;

    @Schema(title = "CREATING / PENDING / READY / FAILED")
    private String status;

    @Schema(title = "本次纳入的订单数(留痕，非运单实际货件数)")
    private Integer orderCount;

    private String fileName;

    private String objectKey;

    @Schema(title = "下载地址，仅 READY 时有值")
    private String downloadUrl;

    private String errorMsg;
}
