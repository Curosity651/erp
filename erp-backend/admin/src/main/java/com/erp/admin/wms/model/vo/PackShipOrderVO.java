package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 打包签出订单（列表 + 详情）。status 已按平台视角映射。
 *
 * @author erp
 */
@Data
@Schema(title = "打包签出订单")
public class PackShipOrderVO {

    private Long id;

    private String outboundNo;

	private String sourceType;

	private Integer salesOrderCount;

	private String platform;

    private Long erpTenantId;

    private String ownerName;

    @Schema(title = "所属WMS服务商ID")
    private Long operatorId;

    @Schema(title = "所属WMS服务商名称")
    private String operatorName;

    private String warehouseName;

    private Integer skuKinds;

    private Integer totalQty;

    @Schema(title = "状态 PICKING/PACKED/SHIPPED/COMPLETED")
    private String status;

    private String pickerName;

    @Schema(title = "打包模式 BY_SKU/BY_ORDER/SECONDARY/CARTON")
    private String packMode;

    private String packerName;

    @Schema(title = "关联物流产品名(计费依据)")
    private String logisticsProductName;

	private String documentMode;

    @Schema(title = "次品/电子类→签出强制拍照")
    private Boolean needPhoto;

    private String channelName;

    private String trackingNo;

    private BigDecimal weight;

    private BigDecimal shippingFee;

    private String createTime;

    @Schema(title = "明细(详情才带)")
    private List<PackShipItemVO> items;

	@Schema(title = "销售出库的平台订单包裹")
	private List<OutboundPackageVO> packages;

	@Schema(title = "仓储作业计费预览")
	private OutboundHandlingPreviewVO handlingPreview;

}
