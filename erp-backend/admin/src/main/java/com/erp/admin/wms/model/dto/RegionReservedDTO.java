package com.erp.admin.wms.model.dto;

import lombok.Data;

/**
 * 区域预占汇总DTO（类型安全，避免 Map<String, Object> 强转）
 */
@Data
public class RegionReservedDTO {

    private Long regionId;
    private Integer totalReserved = 0;
}
