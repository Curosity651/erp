package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

/**
 * 销售出库单详情VO
 *
 * @author erp
 */
@Data
@Schema(title = "销售出库单详情VO")
public class SalesOutboundDetailVO {

    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "出库单号")
    private String outboundNo;

    @Schema(title = "平台：wildberries/ozon/yandex")
    private String platform;

    @Schema(title = "出库仓库ID")
    private Long warehouseId;

    @Schema(title = "出库仓库名称")
    private String warehouseName;

    @Schema(title = "出库日期")
    private LocalDate outboundDate;

    @Schema(title = "订单数量")
    private Integer orderCount;

    @Schema(title = "SKU数量")
    private Integer skuCount;

    @Schema(title = "出库总数量")
    private Integer totalQuantity;

    @Schema(title = "单据状态")
    private String orderStatus;

    @Schema(title = "关联库存过账单ID")
    private Long postingId;

    @Schema(title = "物流产品ID(父服务商提供，签出计费依据)")
    private Long logisticsProductId;

	@Schema(title = "平台资料处理 WAREHOUSE_PRINT/OWNER_PROVIDED")
	private String documentMode;

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

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建人ID")
    private Long createBy;

    @Schema(title = "创建人名称")
    private String createByName;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @Schema(title = "是否存在库存不足的SKU")
    private Boolean hasStockShortage;

    @Schema(title = "库存不足的SKU数量")
    private Integer shortageSkuCount;

    @Schema(title = "出库明细列表")
    private List<SalesOutboundItemVO> items;

}
