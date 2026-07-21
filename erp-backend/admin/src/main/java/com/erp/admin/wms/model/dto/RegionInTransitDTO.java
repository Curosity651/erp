package com.erp.admin.wms.model.dto;

import lombok.Data;

/**
 * 区域级在途库存汇总 DTO
 */
@Data
public class RegionInTransitDTO {
    /** 区域ID */
    private Long regionId;
    /** 区域级在途总量（来自 wms_region_inventory.in_transit_quantity） */
    private Integer totalInTransit;
}
