package com.erp.admin.wms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存过账类型枚举
 * 表示业务事件类型
 */
@Getter
@AllArgsConstructor
public enum PostingType {

    // 采购类
	LOGISTICS_SHIP("物流单发货"),
	LOGISTICS_REDIRECT("物流单目标仓库调整"),
	PURCHASE_RECEIVE("采购到货入库"),

    // 退货类
	RETURN_RECEIVE("退货入库"),

    // 订单类
	SALES_RESERVE("订单占用"),
	SALES_RELEASE("订单释放"),
	SALES_SHIP("销售出库"),

    // 调拨类
	TRANSFER_SHIP("调拨源仓发出"),
	TRANSFER_RECEIVE("调拨目标仓到货"),
    TRANSFER_CANCEL("调拨撤回"),

    // 盘点类
    STOCKTAKE("盘点"),

    // 调整类 - 出库
    SCRAP("报废"),
    OFFLINE_SALE("线下销售"),
    OTHER_OUT("其他出库"),

    // 调整类 - 入库
    OFFLINE_PURCHASE("线下采购"),
    OTHER_IN("其他入库"),

    // 残品类
	TO_DAMAGED("转残品"),
	DAMAGE_DISPOSE("残品消耗");

    private final String description;

}
