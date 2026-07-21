package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购入库单分页视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "采购入库单分页视图对象")
public class PurchaseInboundPageVO {

    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "入库单号")
    private String inboundNo;

    @Schema(title = "退货类型(仅自定义退货单)")
    private String returnType;

    @Schema(title = "关联单号(仅自定义退货单, 纯文本参考)")
    private String refNo;

    @Schema(title = "货主ID")
    private Long erpTenantId;

    @Schema(title = "货主名称")
    private String ownerName;

    @Schema(title = "所属WMS服务商ID")
    private Long operatorId;

    @Schema(title = "所属WMS服务商名称")
    private String operatorName;

    @Schema(title = "关联物流单ID")
    private Long shippingOrderId;

    @Schema(title = "关联物流单号")
    private String shippingOrderNo;

    @Schema(title = "物流商ID（用于Service层聚合）")
    private Long providerId;

    @Schema(title = "物流商名称")
    private String providerName;

    @Schema(title = "入库仓库ID")
    private Long warehouseId;

    @Schema(title = "入库仓库名称")
    private String warehouseName;

    @Schema(title = "入库日期")
    private LocalDate inboundDate;

    @Schema(title = "单据状态")
    private String orderStatus;

    @Schema(title = "入库明细摘要，如 SKU001×100, SKU002×95")
    private String itemSummary;

    @Schema(title = "应到总数量")
    private Integer totalExpectedQty;

    @Schema(title = "实到总数量")
    private Integer totalActualQty;

    @Schema(title = "差异数量（应到-实到）")
    private Integer totalShortQty;

    @Schema(title = "SKU种类数")
    private Integer skuCount;

    @Schema(title = "关联采购单号列表")
    private List<String> purchaseOrderNos;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;

}
