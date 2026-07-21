package com.erp.admin.wms.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台 SKU 销量 DTO（Mapper 查询结果）
 */
@Data
public class PlatformSkuSalesDTO {

    private String platform;

    private String skuCode;

    /** 统计周期内的总销量 */
    private Integer totalQuantity;

    /** 首次出单时间（仅 selectFirstFbsOrderTime 时使用） */
    private LocalDateTime firstOrderTime;
}
