package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 打包签出明细项。
 *
 * @author erp
 */
@Data
@Schema(title = "打包签出明细项")
public class PackShipItemVO {

    @Schema(title = "SKU编码")
    private String skuCode;

    private String warehouseSkuCode;

    @Schema(title = "SKU名称")
    private String skuName;

    @Schema(title = "数量")
    private Integer qty;

    private Integer sortedQty;

    private Integer packedQty;

    @Schema(title = "品质 GOOD/DAMAGED")
    private String quality;

}
