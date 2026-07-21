package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 自定义出库类型枚举
 * <p>
 * 用于标记不挂平台订单的出库业务场景，并映射为对应的库存过账类型。
 * </p>
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum CustomOutboundType {

    /** 线下订单（线下/独立站订单发货） */
    OFFLINE_ORDER("线下订单"),
    /** 样品寄送 */
    SAMPLE_SEND("样品寄送"),
    /** 销毁报废 */
    SCRAP("销毁报废"),
    /** 退供应商 */
    RETURN_TO_SUPPLIER("退供应商"),
    /** 其他 */
    OTHER("其他");

    private final String label;

    /**
     * 映射为库存过账类型（提交预占时使用）
     * @return PostingType 过账类型
     */
    public PostingType toPostingType() {
        switch (this) {
            case OFFLINE_ORDER:
                return PostingType.OFFLINE_SALE;
            case SCRAP:
                return PostingType.SCRAP;
            default:
                // 样品寄送/退供应商/其他 统一归口「其他出库」
                return PostingType.OTHER_OUT;
        }
    }

}
