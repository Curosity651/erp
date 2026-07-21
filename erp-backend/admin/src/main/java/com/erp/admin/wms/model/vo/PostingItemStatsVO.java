package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 过账单明细统计VO
 *
 * @author erp
 */
@Data
@Schema(title = "过账单明细统计VO")
public class PostingItemStatsVO {

    @Schema(title = "过账单ID")
    private Long postingId;

    @Schema(title = "SKU种类数")
    private Integer skuCount;

    @Schema(title = "总数量")
    private Integer totalQuantity;

}
