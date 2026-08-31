package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存流水分页VO
 *
 * @author erp
 */
@Data
@Schema(title = "库存流水分页VO")
public class StockFlowPageVO {

    @Schema(title = "流水ID")
    private Long id;

    @Schema(title = "仓库ID")
    private Long warehouseId;

    @Schema(title = "仓库展示信息")
    private WarehouseDisplayVO warehouseDisplay;

    @Schema(title = "区域ID（区域级流水时 > 0，仓库级流水时 = 0）")
    private Long regionId;

    @Schema(title = "区域展示信息")
    private RegionDisplayVO regionDisplay;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "SKU简要信息")
    private SkuBriefVO skuBrief;

    @Schema(title = "库存桶: AVAILABLE/RESERVED/IN_TRANSIT/DAMAGED/SCRAP")
    private String bucket;

    @Schema(title = "方向: IN/OUT")
    private String direction;

    @Schema(title = "变更数量(正数)")
    private Integer quantity;

    @Schema(title = "变动前数量")
    private Integer beforeQuantity;

    @Schema(title = "变动后数量")
    private Integer afterQuantity;

    @Schema(title = "过账单ID")
    private Long postingId;

    @Schema(title = "过账单号")
    private String postingNo;

    @Schema(title = "过账单明细ID")
    private Long postingItemId;

    @Schema(title = "过账类型")
    private String postingType;

    @Schema(title = "来源单据类型")
    private String sourceType;

    @Schema(title = "来源单据ID")
    private Long sourceId;

    @Schema(title = "来源单据号")
    private String sourceNo;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "操作人ID")
    private Long createBy;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    private Long locationId;
    private String locationCode;
    private Long counterpartLocationId;
    private String counterpartLocationCode;
    private String quality;
    private Integer quantityDelta;
    private Integer reservedDelta;
    private Integer beforeReserved;
    private Integer afterReserved;

}
