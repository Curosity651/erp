package com.erp.admin.order.service.yandex.converter;

import com.erp.admin.order.model.enums.ErpOrderStatusEnum;
import com.erp.admin.platform.yandex.enums.YandexOrderStatusEnum;
import com.erp.admin.platform.yandex.enums.YandexOrderSubstatusEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * Yandex 订单状态 → 统一 ERP 状态映射工具。
 * <p>
 * 映射规则基于 Yandex Market Partner API 文档中 OrderStatusType 定义：
 * <ul>
 *   <li>PLACING / RESERVED / UNPAID / UNKNOWN → null（忽略，不可操作）</li>
 *   <li>PENDING → READY_TO_SHIP（待卖家处理）</li>
 *   <li>PROCESSING + substatus=READY_TO_SHIP/SHIPPED → SHIPPED（备货完成/已交运，进入发货中）</li>
 *   <li>PROCESSING 其他 substatus（STARTED 等） → READY_TO_SHIP（待发货）</li>
 *   <li>DELIVERY → SHIPPED（配送中）</li>
 *   <li>PICKUP → ARRIVED_AT_PLATFORM_WAREHOUSE（已到取货点）</li>
 *   <li>DELIVERED / PARTIALLY_RETURNED → DELIVERED（已签收）</li>
 *   <li>CANCELLED → CANCELED（已取消）</li>
 *   <li>RETURNED → RETURNED（已退货）</li>
 * </ul>
 */
@Slf4j
public final class YandexOrderStatusConverter {

	private YandexOrderStatusConverter() {
	}

	/**
	 * 将 Yandex 订单状态转换为 ERP 统一状态。
	 *
	 * @param status    Yandex 主状态字符串
	 * @param substatus Yandex 子状态字符串（当前映射不依赖，保留用于日志）
	 * @return ERP 状态枚举，null 表示应忽略该订单
	 */
	public static ErpOrderStatusEnum toErpStatus(String status, String substatus) {
		if (status == null) {
			return null;
		}
		YandexOrderStatusEnum statusEnum;
		try {
			statusEnum = YandexOrderStatusEnum.valueOf(status);
		} catch (IllegalArgumentException e) {
			log.warn("[YANDEX] 未知订单状态: status={}, substatus={}", status, substatus);
			return null;
		}

		switch (statusEnum) {
			// 1️⃣ 已取消
			case CANCELLED:
				return ErpOrderStatusEnum.CANCELED;

			// 2️⃣ 已退货
			case RETURNED:
				return ErpOrderStatusEnum.RETURNED;

			// 3️⃣ 已签收（含部分退货）
			case DELIVERED:
			case PARTIALLY_RETURNED:
				return ErpOrderStatusEnum.DELIVERED;

			// 4️⃣ 已到取货点（对齐 WB SORTED/READY_FOR_PICKUP）
			case PICKUP:
				return ErpOrderStatusEnum.ARRIVED_AT_PLATFORM_WAREHOUSE;

			// 5️⃣ 配送中
			case DELIVERY:
				return ErpOrderStatusEnum.SHIPPED;

			// 6️⃣ 待发货（PENDING=等待卖家处理）
			case PENDING:
				return ErpOrderStatusEnum.READY_TO_SHIP;

			// PROCESSING=处理中，substatus 表示处理阶段：STARTED → READY_TO_SHIP → SHIPPED
			case PROCESSING:
				// READY_TO_SHIP(备货完成)/SHIPPED(已交给配送服务) 均已过确认环节 → ERP 发货中；
				// 不能让 SHIPPED 落入 else 回退成待发货，否则已交运订单会被打回并允许二次确认
				if (YandexOrderSubstatusEnum.READY_TO_SHIP.name().equals(substatus)
						|| YandexOrderSubstatusEnum.SHIPPED.name().equals(substatus)) {
					return ErpOrderStatusEnum.SHIPPED;
				} else {
					return ErpOrderStatusEnum.READY_TO_SHIP;
				}

			// 7️⃣ 已知的不可操作状态 → 静默忽略（未过滤拉取时属正常数据，不刷告警）
			case PLACING:
			case RESERVED:
			case UNPAID:
				log.debug("[YANDEX] 忽略不可操作状态: status={}, substatus={}", status, substatus);
				return null;

			// 意料之外的状态 → 告警提示需要补充映射
			case UNKNOWN:
			default:
				log.warn("[YANDEX] 忽略未映射状态: status={}, substatus={}", status, substatus);
				return null;
		}
	}
}
