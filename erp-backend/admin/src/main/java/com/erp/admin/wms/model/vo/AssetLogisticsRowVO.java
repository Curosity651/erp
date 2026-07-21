package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 物流附加成本明细行（按 SKU，原币 USD）。仅已发运在库量（A+B+C）承担物流附加。
 *
 * @author erp
 */
@Data
@Builder
public class AssetLogisticsRowVO {

    private String skuCode;
    private SkuBriefVO skuBrief;

    /** 已发运在库量（A+B+C） */
    private int shippedHeldQty;

    /** 物流单位成本 USD = 分摊物流费 ÷ 发运量 */
    private BigDecimal unitCostUsd;

    /** 物流附加成本 USD = shippedHeldQty × unitCostUsd */
    private BigDecimal logisticsCostUsd;
}
