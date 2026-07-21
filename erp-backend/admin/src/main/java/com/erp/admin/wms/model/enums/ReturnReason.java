package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 退货原因枚举
 */
@Getter
@AllArgsConstructor
public enum ReturnReason {

    NOT_WANTED("客户不想要了"),
    DAMAGED("商品破损"),
    WRONG_ITEM("发错货"),
    QUALITY_ISSUE("质量问题"),
    OTHER("其他");

    private final String description;

}
