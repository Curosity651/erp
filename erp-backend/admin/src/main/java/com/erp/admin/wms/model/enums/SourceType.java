package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 过账/流水来源类型
 *
 * @author erp
 */
@Getter
@AllArgsConstructor
public enum SourceType {

    // 采购链条
    SHIPPING_ORDER("物流单"),
    PURCHASE_INBOUND("采购入库单"),

    // 销售链条
    ORDER("销售订单"),
    SALES_OUTBOUND("销售出库单"),

    // 自定义出库（线下订单/样品/销毁/退供应商等，不挂平台订单）
    CUSTOM_OUTBOUND("自定义出库单"),

    // 退货
    RETURN_INBOUND("退货入库单"),

    // 调拨
    TRANSFER("调拨单"),

    // 盘点/调整
    STOCKTAKE("盘点单"),
    ADJUSTMENT("调整单"),

    // 库内移库（库位调整，同仓库位间移动）
    LOCATION_TRANSFER("库位调整");

    private final String label;

}
