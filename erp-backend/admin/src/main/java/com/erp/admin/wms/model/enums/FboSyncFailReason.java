package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * FBO同步失败原因枚举
 */
@Getter
@AllArgsConstructor
public enum FboSyncFailReason {

    SKU_NOT_MAPPED("SKU_NOT_MAPPED", "SKU未映射"),
    WAREHOUSE_NOT_FOUND("WAREHOUSE_NOT_FOUND", "仓库未配置"),
    API_ERROR("API_ERROR", "API调用错误"),
    DATA_PARSE_ERROR("DATA_PARSE_ERROR", "数据解析错误");

    private final String code;
    private final String name;

}
