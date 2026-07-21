package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 盘点差异明细项VO
 *
 * @author erp
 */
@Data
@Schema(description = "盘点差异明细项")
public class StocktakeDiffItemVO {

    @Schema(description = "SKU编码")
    private String skuCode;

    @Schema(description = "SKU简要信息")
    private SkuBriefVO skuBrief;

    @Schema(description = "系统数量")
    private Integer systemQuantity;

    @Schema(description = "实盘数量")
    private Integer actualQuantity;

    @Schema(description = "差异数量（实盘-系统，正为盘盈，负为盘亏）")
    private Integer diffQuantity;

    @Schema(description = "差异类型：PROFIT-盘盈，LOSS-盘亏")
    private String diffType;

}
