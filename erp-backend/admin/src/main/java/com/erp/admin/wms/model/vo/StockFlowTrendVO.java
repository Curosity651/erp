package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 库存流水趋势数据 VO
 *
 * @author erp
 */
@Data
@Schema(title = "库存流水趋势数据")
public class StockFlowTrendVO {

    @Schema(title = "日期", example = "2025-01-17")
    private String date;

    @Schema(title = "入库数量")
    private Integer inQuantity;

    @Schema(title = "出库数量")
    private Integer outQuantity;

}
