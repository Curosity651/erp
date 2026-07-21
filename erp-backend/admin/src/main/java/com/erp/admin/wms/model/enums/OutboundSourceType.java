package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 出库单来源类型枚举
 * <p>
 * 销售出库单与自定义出库单共用 {@code wms_sales_outbound_order} 表，
 * 通过 {@code source_type} 区分（对齐入库侧 {@code InboundSourceType} 的先例）。
 * </p>
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum OutboundSourceType {

    /** 销售出库（关联平台订单） */
    SALES("销售出库"),
    /** 自定义出库（不挂平台订单：线下订单/样品/销毁/退供应商等） */
    CUSTOM("自定义出库");

    private final String label;

}
