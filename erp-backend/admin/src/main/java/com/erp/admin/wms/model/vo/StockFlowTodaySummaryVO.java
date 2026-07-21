package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 库存流水今日汇总 VO
 *
 * @author erp
 */
@Data
@Schema(title = "库存流水今日汇总")
public class StockFlowTodaySummaryVO {

    @Schema(title = "今日入库数量")
    private Integer inQuantity;

    @Schema(title = "今日出库数量")
    private Integer outQuantity;

    @Schema(title = "今日预占数量")
    private Integer reserveQuantity;

    @Schema(title = "今日释放数量")
    private Integer releaseQuantity;

}
