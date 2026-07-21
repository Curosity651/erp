package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 采购入库单状态枚举
 */
@Getter
@AllArgsConstructor
public enum PurchaseInboundStatus {

    DRAFT("草稿"),
    SUBMITTED("已提交"),
    RECEIVED("已收货"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String description;

}
