package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.math.BigDecimal;

/**
 * 自定义出库单详情VO
 *
 * @author erp
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(title = "自定义出库单详情VO")
public class CustomOutboundDetailVO extends CustomOutboundPageVO {

    @Schema(title = "收件人姓名")
    private String receiverName;

    @Schema(title = "收件人电话")
    private String receiverPhone;

    @Schema(title = "收货地址")
    private String receiverAddress;

    @Schema(title = "关联库存过账单ID")
    private Long postingId;

    @Schema(title = "物流产品ID")
    private Long logisticsProductId;

    @Schema(title = "物流产品名称")
    private String logisticsProductName;

    @Schema(title = "签出物流渠道")
    private String channelName;

    @Schema(title = "运单号（选填）")
    private String trackingNo;

    @Schema(title = "签出重量kg")
    private BigDecimal weight;

    @Schema(title = "物流费用")
    private BigDecimal shippingFee;

    @Schema(title = "出库明细")
    private List<CustomOutboundItemVO> items;

    @Schema(title = "是否存在库存不足（草稿态计算）")
    private Boolean hasStockShortage;

    @Schema(title = "库存不足的SKU数量（草稿态计算）")
    private Integer shortageSkuCount;

}
