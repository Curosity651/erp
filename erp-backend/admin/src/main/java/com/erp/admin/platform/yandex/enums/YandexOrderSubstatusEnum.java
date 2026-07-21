package com.erp.admin.platform.yandex.enums;

/**
 * Yandex Market 订单子状态枚举
 * <p>
 * substatus 在 status=PROCESSING 时表示处理阶段，在 status=CANCELLED 时表示取消原因
 */
public enum YandexOrderSubstatusEnum {

	// ======== PROCESSING 阶段 ========

	/** 订单已确认，可以开始处理 */
	STARTED,

	/** 订单已准备好发货 */
	READY_TO_SHIP,

	/** 订单已交给配送服务 */
	SHIPPED,

	// ======== CANCELLED 原因 ========

	/** 预留过期（买家10分钟内未完成下单） */
	RESERVATION_EXPIRED,

	/** 买家未支付（预付订单30分钟内未支付） */
	USER_NOT_PAID,

	/** 无法联系买家 */
	USER_UNREACHABLE,

	/** 买家主动取消 */
	USER_CHANGED_MIND,

	/** 买家不满意配送条件 */
	USER_REFUSED_DELIVERY,

	/** 买家不满意商品 */
	USER_REFUSED_PRODUCT,

	/** 商家无法履约 */
	SHOP_FAILED,

	/** 买家不满意商品质量 */
	USER_REFUSED_QUALITY,

	/** 买家要求换货 */
	REPLACING_ORDER,

	/** 处理超时（已废弃） */
	PROCESSING_EXPIRED,

	/** 取货点存储到期 */
	PICKUP_EXPIRED,

	/** 配送服务无法送达 */
	DELIVERY_SERVICE_UNDELIVERED,

	/** 找不到快递员 */
	CANCELLED_COURIER_NOT_FOUND,

	/** 买家想更换配送日期 */
	USER_WANTS_TO_CHANGE_DELIVERY_DATE,

	/** 平台无法继续处理订单 */
	RESERVATION_FAILED;
}
