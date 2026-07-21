package com.erp.admin.shop.model.enums;

/**
 * 店铺状态
 * 0=禁用 1=正常 2=异常
 */
public enum ShopStatusEnum {

    DISABLED(0),
    ENABLED(1),
    ABNORMAL(2);

    private final int code;

    ShopStatusEnum(int code) { this.code = code; }
    public int getCode() { return code; }

    public static ShopStatusEnum fromCode(Integer code) {
        if (code == null) return null;
        for (ShopStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        return null;
    }
}
