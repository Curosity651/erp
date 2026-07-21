package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 采购单发货状态枚举
 * <p>
 * 由物流单确认发货时自动更新
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum PurchaseShippingStatus {

    NOT_SHIPPED("未发货"),
    PARTIAL_SHIPPED("部分发货"),
    ALL_SHIPPED("全部发货");

    private final String description;

    /**
     * 根据发货数量计算发货状态
     * @param shippedQuantity 已发货数量
     * @param totalQuantity 总数量
     * @return 发货状态
     */
    public static PurchaseShippingStatus calculate(int shippedQuantity, int totalQuantity) {
        if (shippedQuantity <= 0) {
            return NOT_SHIPPED;
        } else if (shippedQuantity >= totalQuantity) {
            return ALL_SHIPPED;
        } else {
            return PARTIAL_SHIPPED;
        }
    }

}
