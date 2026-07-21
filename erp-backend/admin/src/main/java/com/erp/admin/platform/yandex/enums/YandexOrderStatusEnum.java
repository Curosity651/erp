package com.erp.admin.platform.yandex.enums;

/**
 * Yandex Market 订单主状态枚举
 * <p>
 * 文档：https://yandex.ru/dev/market/partner-api/doc/ru/reference/orders
 */
public enum YandexOrderStatusEnum {

	/** 正在下单，准备预留 */
	PLACING,

	/** 已预留但未完成下单 */
	RESERVED,

	/** 订单已创建，等待买家支付 */
	UNPAID,

	/** 等待卖家处理 */
	PENDING,

	/** 订单正在处理中（已支付，商家可操作） */
	PROCESSING,

	/** 订单已移交配送服务 */
	DELIVERY,

	/** 订单已到达取货点 */
	PICKUP,

	/** 订单已签收 */
	DELIVERED,

	/** 订单部分退货 */
	PARTIALLY_RETURNED,

	/** 订单已退货 */
	RETURNED,

	/** 订单已取消 */
	CANCELLED,

	/** 未知状态 */
	UNKNOWN;
}
