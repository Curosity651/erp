package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 调整类型枚举
 * 每种类型绑定 PostingType 和扣减桶
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum AdjustmentType {

    // 报废（唯一保留类型）：扣减可用库存。原线下销售/其他出库/线下采购/其他入库/转残品/残品消耗
    // 均为 ERP+OMS 改造残留，出入库统一由 ERP+OMS 产生，已删除（2026-07）。
    SCRAP("报废", PostingType.SCRAP, StockBucket.AVAILABLE);

    private final String description;

    private final PostingType postingType;

    /**
     * 扣减桶，用于校验和前端展示"当前库存"
     * null 表示入库类，无需校验
     */
    private final StockBucket deductBucket;

    /**
     * 是否需要扣减校验
     */
    public boolean requiresDeduction() {
        return deductBucket != null;
    }

}
