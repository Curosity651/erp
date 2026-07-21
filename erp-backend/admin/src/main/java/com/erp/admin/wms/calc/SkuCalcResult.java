package com.erp.admin.wms.calc;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 单 SKU 测算汇总结果（移植自 model_core.SkuResult）。
 *
 * @author erp
 */
@Data
@Builder
public class SkuCalcResult {

    private SkuCalcInput input;

    private SalesMetrics sales;

    private ShippingResult shipping;

    private ProductionResult production;

    /** 数据可信度告警（新品 / 历史不足 / 零销量等） */
    private List<String> warnings;
}
