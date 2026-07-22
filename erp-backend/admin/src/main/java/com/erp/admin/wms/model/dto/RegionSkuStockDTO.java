package com.erp.admin.wms.model.dto;

import lombok.Data;

/**
 * 区域 SKU 库存聚合 DTO（Mapper 查询结果）
 */
@Data
public class RegionSkuStockDTO {

    private Long regionId;

    private String skuCode;

    /** 可用库存汇总 = SUM(区域内自有仓.available) */
    private Integer totalAvailable;

    /** 预占库存汇总 = SUM(区域内自有仓.reserved) */
    private Integer totalReserved;

    /** 在途库存汇总 = SUM(区域内自有仓.in_transit) */
    private Integer totalInTransit;

    /** 残品库存汇总 = SUM(区域内自有仓.damaged) */
    private Integer totalDamaged;

    public static RegionSkuStockDTO empty(Long regionId, String skuCode) {
        RegionSkuStockDTO dto = new RegionSkuStockDTO();
        dto.setRegionId(regionId);
        dto.setSkuCode(skuCode);
        dto.setTotalAvailable(0);
        dto.setTotalReserved(0);
        dto.setTotalInTransit(0);
        dto.setTotalDamaged(0);
        return dto;
    }
}
