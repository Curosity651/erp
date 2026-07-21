package com.erp.admin.order.model.vo;

import lombok.Data;

/**
 * SKU 日销量统计 VO
 * <p>
 * 用于替代 Map<String, Object> 返回销量统计，提供类型安全
 *
 * @author erp
 */
@Data
public class SkuDailySalesVO {

    /** SKU编码 */
    private String skuCode;

    /** 统计周期内总销量 */
    private Long totalQuantity;
}
