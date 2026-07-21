package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存变更方向枚举
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum StockDirection {

    IN("入库"),
    OUT("出库");

    private final String label;

}
