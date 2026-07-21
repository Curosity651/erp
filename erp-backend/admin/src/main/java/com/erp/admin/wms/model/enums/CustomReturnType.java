package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 自定义退货单退货类型（{@code wms_purchase_inbound_order.return_type}，仅 source_type=CUSTOM_RETURN 时使用）。
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum CustomReturnType {

    PLATFORM_BATCH("平台批量退货"),
    NO_ORDER("无单退件"),
    SAMPLE_BACK("样品收回"),
    WRONG_SHIPMENT("发错召回"),
    OTHER("其他");

    private final String description;

    /**
     * 判断退货类型编码是否合法
     * @param name 退货类型编码
     * @return true-合法
     */
    public static boolean isValid(String name) {
        for (CustomReturnType type : values()) {
            if (type.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

}
