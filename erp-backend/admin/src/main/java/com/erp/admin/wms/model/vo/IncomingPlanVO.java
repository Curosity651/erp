package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 入库计划VO
 *
 * @author erp
 */
@Data
@Schema(title = "入库计划VO")
public class IncomingPlanVO {

    @Schema(title = "物流单ID")
    private Long shippingOrderId;

    @Schema(title = "物流单号")
    private String shippingNo;

    @Schema(title = "预计到达日期")
    private LocalDate estimatedArrivalDate;

    @Schema(title = "数量")
    private Integer quantity;
}
