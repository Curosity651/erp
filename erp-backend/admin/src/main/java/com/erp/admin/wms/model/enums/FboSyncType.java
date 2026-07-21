package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * FBO同步类型枚举
 */
@Getter
@AllArgsConstructor
public enum FboSyncType {

    SCHEDULED("SCHEDULED", "定时同步"),
    MANUAL("MANUAL", "手动同步");

    private final String code;
    private final String name;

    public static FboSyncType fromCode(String code) {
        for (FboSyncType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown FboSyncType code: " + code);
    }

}
