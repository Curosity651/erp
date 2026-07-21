package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购成本明细行（按 SKU × 币种）。持有量按各币种采购量占比拆分（加权平均近似）。
 *
 * @author erp
 */
@Data
@Builder
public class AssetProcurementRowVO {

    private String skuCode;
    private SkuBriefVO skuBrief;
    private String currency;

    /** 归入该币种的持有量（A–E 全链路，按采购量占比拆分） */
    private int heldQty;

    /** 加权平均采购单价 */
    private BigDecimal avgUnitPrice;

    /** 采购成本 = heldQty × avgUnitPrice */
    private BigDecimal procurementCost;
}
