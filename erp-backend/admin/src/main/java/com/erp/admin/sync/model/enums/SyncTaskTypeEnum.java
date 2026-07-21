package com.erp.admin.sync.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SyncTaskTypeEnum {

    ORDER_INCREMENTAL("订单增量同步"),

    ORDER_STATUS("未完结订单状态回溯");

    private final String description;
}
