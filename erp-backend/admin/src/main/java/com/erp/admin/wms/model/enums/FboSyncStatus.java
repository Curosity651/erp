package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * FBO同步状态枚举
 */
@Getter
@AllArgsConstructor
public enum FboSyncStatus {

    SUCCESS("SUCCESS", "成功"),
    PARTIAL("PARTIAL", "部分成功"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String name;

    public static FboSyncStatus fromCode(String code) {
        for (FboSyncStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown FboSyncStatus code: " + code);
    }

}
