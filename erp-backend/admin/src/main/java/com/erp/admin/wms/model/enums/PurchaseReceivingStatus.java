package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 采购单入库状态枚举
 * <p>
 * 由采购入库单确认时自动更新
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum PurchaseReceivingStatus {

    NOT_RECEIVED("未入库"),
    PARTIAL_RECEIVED("部分入库"),
    ALL_RECEIVED("全部入库");

    private final String description;

    /**
     * 根据入库数量计算入库状态
     * @param receivedQuantity 已入库数量
     * @param totalQuantity 总数量
     * @return 入库状态
     */
    public static PurchaseReceivingStatus calculate(int receivedQuantity, int totalQuantity) {
        if (receivedQuantity <= 0) {
            return NOT_RECEIVED;
        } else if (receivedQuantity >= totalQuantity) {
            return ALL_RECEIVED;
        } else {
            return PARTIAL_RECEIVED;
        }
    }

}
