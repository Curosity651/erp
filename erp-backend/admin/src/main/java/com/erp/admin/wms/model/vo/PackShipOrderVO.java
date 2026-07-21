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

    @Schema(title = "次品/电子类→签出强制拍照")
    private Boolean needPhoto;

    private String channelName;

    private String trackingNo;

    private BigDecimal weight;

    private BigDecimal shippingFee;

    private String createTime;

    @Schema(title = "明细(详情才带)")
    private List<PackShipItemVO> items;

}
