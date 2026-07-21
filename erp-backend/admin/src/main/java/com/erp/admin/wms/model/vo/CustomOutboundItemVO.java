package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 自定义出库单明细VO
 *
 * @author erp
 */
@Data
@Schema(title = "自定义出库单明细VO")
public class CustomOutboundItemVO {

    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "出库单ID")
    private Long outboundOrderId;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "SKU简要信息")
    private SkuBriefVO skuBrief;

    @Schema(title = "出库数量")
    private Integer quantity;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "可用库存数量（草稿态计算）")
    private Integer availableStock;

    @Schema(title = "库存缺口（0表示充足）")
    private Integer shortage;

    @Schema(title = "库存状态：sufficient/insufficient/zero/deducted")
    private String stockStatus;

}
