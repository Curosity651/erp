package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 库存过账单明细VO
 *
 * @author erp
 */
@Data
@Schema(title = "库存过账单明细VO")
public class StockPostingItemVO {

    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "过账单ID")
    private Long postingId;

    @Schema(title = "仓库ID（仓库级操作时 > 0，区域级操作时 = 0）")
    private Long warehouseId;

    @Schema(title = "区域ID（区域级操作时 > 0，仓库级操作时 = 0）")
    private Long regionId;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "SKU简要信息")
    private SkuBriefVO skuBrief;

    @Schema(title = "库存桶: AVAILABLE/RESERVED/IN_TRANSIT/DAMAGED/SCRAP")
    private String bucket;

    @Schema(title = "方向: IN/OUT")
    private String direction;

    @Schema(title = "数量")
    private Integer quantity;

    @Schema(title = "仓库展示信息")
    private WarehouseDisplayVO warehouseDisplay;

    @Schema(title = "区域展示信息")
    private RegionDisplayVO regionDisplay;

    private Long locationId;
    private String locationCode;
    private Long counterpartLocationId;
    private String counterpartLocationCode;
    private String quality;
    private Integer quantityDelta;
    private Integer reservedDelta;
    private Integer beforeQuantity;
    private Integer afterQuantity;
    private Integer beforeReserved;
    private Integer afterReserved;

}
