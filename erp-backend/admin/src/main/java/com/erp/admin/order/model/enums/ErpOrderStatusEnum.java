package com.erp.admin.order.model.enums;

/**
 * ERP 统一订单状态枚举。
 * 枚举值：READY_TO_SHIP|SHIPPED|DELIVERED|CANCELED|RETURNED
 */
public enum ErpOrderStatusEnum {
    /** 已确认 */
    READY_TO_SHIP,
    /** 发货中/已发货/运输中 */
    SHIPPED,
	/** 已送达平台仓库 */
	ARRIVED_AT_PLATFORM_WAREHOUSE,
    /** 已送达/已收货 */
    DELIVERED,
    /** 已取消 */
    CANCELED,
    /** 已退货/拒收 */
    RETURNED;
}


