package com.erp.admin.order.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单出库状态枚举
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum OutboundStatus {

    NONE("未出库"),
    ALLOCATED("已分配"),
    COMPLETED("已出库");

    private final String description;

}
