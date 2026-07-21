package com.erp.admin.wms.model.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * SKU 逐日销量（FBS，全平台汇总）——发货生产测算 Yt/Xt 的取数底座。
 *
 * @author erp
 */
@Data
public class SkuDailySalesDTO {

    private String skuCode;

    /** 销售日期（platform_created_at 的日期部分） */
    private LocalDate saleDate;

    /** 当日销量 */
    private Integer quantity;
}
