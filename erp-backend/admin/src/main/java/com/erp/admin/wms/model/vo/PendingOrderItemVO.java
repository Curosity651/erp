package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 待出库订单明细VO
 *
 * @author erp
 */
@Data
@Schema(title = "待出库订单明细VO")
public class PendingOrderItemVO {

    @Schema(title = "平台商品ID")
    private String platformItemId;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "SKU简要信息")
    private SkuBriefVO skuBrief;

    @Schema(title = "数量")
    private Integer quantity;

    @Schema(title = "可用库存数量")
    private Integer availableStock;

    @Schema(title = "库存状态")
    private String stockStatus;

}
