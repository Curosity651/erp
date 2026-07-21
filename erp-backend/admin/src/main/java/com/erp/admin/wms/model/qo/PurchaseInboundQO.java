package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * 采购入库单查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "采购入库单查询对象")
public class PurchaseInboundQO {

    @Schema(title = "入库单号")
    private String inboundNo;

    @Schema(title = "关联物流单号")
    private String shippingOrderNo;

    @Schema(title = "采购单号")
    private String purchaseOrderNo;

    @Schema(title = "入库仓库ID")
    private Long warehouseId;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "单据状态(单值)")
    private String orderStatus;

    @Schema(title = "单据状态(多值, 命中其一即可; 用于平台收货/上架页按阶段过滤)")
    private List<String> orderStatuses;

    @Schema(title = "来源: PURCHASE采购 / MANUAL自定义 / CUSTOM_RETURN自定义退货（由后端按页面强制设置）")
    private String sourceType;

    @Schema(title = "退货类型(仅自定义退货页使用)")
    private String returnType;

    @Schema(title = "货主作用域(货主身份由后端强制为自身; 平台身份可作为筛选条件, null=看全部)")
    private Long erpTenantId;

    @Schema(title = "WMS服务商筛选(仅平台身份有效; 按入库单归属货主的上级服务商过滤)")
    private Long wmsTenantId;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(title = "入库日期起始")
    private LocalDate inboundDateStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(title = "入库日期结束")
    private LocalDate inboundDateEnd;

}
