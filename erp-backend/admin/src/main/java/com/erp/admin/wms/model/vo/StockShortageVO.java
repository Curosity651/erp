package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 库存不足明细 VO
 */
@Data
@Schema(title = "库存不足明细VO")
public class StockShortageVO {

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "SKU名称")
    private String skuName;

    @Schema(title = "需求数量")
    private Integer requiredQty;

    @Schema(title = "可用库存")
    private Integer availableQty;

    @Schema(title = "缺口数量")
    private Integer shortage;
}
