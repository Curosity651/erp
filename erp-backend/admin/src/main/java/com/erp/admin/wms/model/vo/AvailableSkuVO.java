package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 可用SKU视图对象 (用于盘点预览等场景)
 *
 * @author erp
 */
@Data
@Schema(title = "可用SKU视图对象")
public class AvailableSkuVO {

    @Schema(description = "SKU编码")
    private String skuCode;

    private String warehouseSkuCode;

    @Schema(description = "货主ID")
    private Long erpTenantId;

    @Schema(description = "货主名称")
    private String ownerName;

    @Schema(description = "SKU简要信息")
    private SkuBriefVO skuBrief;

    @Schema(description = "当前库存数量")
    private Integer stockQuantity;
}
