package com.erp.admin.wms.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 库存状态枚举
 */
@Getter
@RequiredArgsConstructor
public enum StockStatus {

    /**
     * 充足
     */
    SUFFICIENT("sufficient", "充足"),

    /**
     * 不足
     */
    INSUFFICIENT("insufficient", "不足"),

    /**
     * 无库存
     */
    ZERO("zero", "无库存"),

    /**
     * 已扣减
     */
    DEDUCTED("deducted", "已扣减");

    private final String value;
    private final String desc;

    /**
     * 根据可用库存和需求数量计算库存状态
     *
     * @param available 可用库存
     * @param required  需求数量
     * @return 库存状态
     */
    public static StockStatus calc(int available, int required) {
        if (available >= required) {
            return SUFFICIENT;
        } else if (available > 0) {
            return INSUFFICIENT;
        } else {
            return ZERO;
        }
    }

    /**
     * 计算库存缺口
     *
     * @param available 可用库存
     * @param required  需求数量
     * @return 缺口数量（>=0）
     */
    public static int calcShortage(int available, int required) {
        return Math.max(0, required - available);
    }
}
