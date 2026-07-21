package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售出库单分页VO
 *
 * @author erp
 */
@Data
@Schema(title = "销售出库单分页VO")
public class SalesOutboundPageVO {

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

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建人ID")
    private Long createBy;

    @Schema(title = "创建人名称")
    private String createByName;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;

}
