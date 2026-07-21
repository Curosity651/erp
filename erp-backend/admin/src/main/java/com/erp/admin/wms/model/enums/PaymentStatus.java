package com.erp.admin.wms.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 付款状态枚举
 *
 * @author erp
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    UNPAID(0, "未付"),
    PAID(1, "已付");

    private final Integer value;
    private final String desc;

    /**
     * 根据值获取枚举
     * @param value 值
     * @return 枚举实例
     */
    public static PaymentStatus fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (PaymentStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的付款状态: " + value);
    }

    /**
     * 判断是否已付款
     * @param value 状态值
     * @return 是否已付款
     */
    public static boolean isPaid(Integer value) {
        return PAID.value.equals(value);
    }

}
