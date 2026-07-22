package com.erp.admin.wms.model.dto;

import lombok.Data;

/**
 * 仓库库存聚合DTO（内部使用，用于区域汇总计算）
 */
@Data
public class WarehouseAggregateDTO {

    private Long regionId;
    private Integer warehouseCount = 0;
    private Integer totalAvailable = 0;

    private Integer totalReserved = 0;

    private Integer totalInTransit = 0;
    private Integer totalDamaged = 0;

    /**
     * 创建空的聚合对象（避免静态可变对象的线程安全问题）
     */
    public static WarehouseAggregateDTO empty() {
        return new WarehouseAggregateDTO();
    }
}
