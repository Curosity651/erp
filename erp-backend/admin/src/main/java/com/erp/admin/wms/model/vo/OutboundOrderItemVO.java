package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 出库单明细（下架视角：需求数 / 可用良品 / 缺货标记）。
 *
 * @author erp
 */
@Data
@Schema(title = "出库单明细(下架)")
public class OutboundOrderItemVO {

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "SKU名称")
    private String skuName;

    @Schema(title = "需求数")
    private Integer requiredQty;

    @Schema(title = "可用良品库存(GOOD 且 allocatable)")
    private Integer availableQty;

    @Schema(title = "缺货标记(availableQty < requiredQty)")
    private Boolean shortage;

}
