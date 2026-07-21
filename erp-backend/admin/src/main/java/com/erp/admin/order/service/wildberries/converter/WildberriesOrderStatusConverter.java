package com.erp.admin.order.service.wildberries.converter;

import com.erp.admin.order.model.enums.ErpOrderStatusEnum;
import com.erp.admin.platform.wildberries.enums.WildberriesSupplierStatusEnum;
import com.erp.admin.platform.wildberries.enums.WildberriesWbStatusEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * Wildberries 履约状态（supplierStatus） -> 统一 erpStatus 映射工具。
 * 统一状态集合: PENDING|READY_TO_SHIP|SHIPPED|DELIVERED|CANCELED|RETURNED
 * 说明: 仅依据 supplierStatus 进行映射；未知状态采用『宽松』策略映射为 PENDING。
 */
@Slf4j
public final class WildberriesOrderStatusConverter {

	private WildberriesOrderStatusConverter() {
	}

	/**
	 * 基于 supplierStatus + wbStatus 转换为统一 ERP 状态。
	 *
	 * @param supplierStatus 商家侧状态，可为 null
	 * @param wbStatus       平台侧状态，可为 null
	 * @return 统一 ERP 状态
	 */
	public static ErpOrderStatusEnum toErpStatus(WildberriesSupplierStatusEnum supplierStatus,
												 WildberriesWbStatusEnum wbStatus) {

		// 统一判空逻辑
		String supplier = supplierStatus == null ? "" : supplierStatus.name();
		String wb = wbStatus == null ? "" : wbStatus.name();

		// 1️⃣ 已取消 / 拒收 / 缺陷
		if (supplierStatus == WildberriesSupplierStatusEnum.CANCEL
				|| wbStatus == WildberriesWbStatusEnum.CANCELED
				|| wbStatus == WildberriesWbStatusEnum.CANCELED_BY_CLIENT
				|| wbStatus == WildberriesWbStatusEnum.DECLINED_BY_CLIENT) {
			return ErpOrderStatusEnum.CANCELED;
		}
		if (supplierStatus == WildberriesSupplierStatusEnum.REJECT
				|| wbStatus == WildberriesWbStatusEnum.DEFECT) {
			return ErpOrderStatusEnum.RETURNED;
		}

		// 2️⃣ 已交付（买家收货）
		if (supplierStatus == WildberriesSupplierStatusEnum.RECEIVE
				|| wbStatus == WildberriesWbStatusEnum.SOLD) {
			return ErpOrderStatusEnum.DELIVERED;
		}

		// 3️⃣ 已发货 / 已送仓
		if (supplierStatus == WildberriesSupplierStatusEnum.COMPLETE) {
			if (wbStatus == WildberriesWbStatusEnum.WAITING) {
				return ErpOrderStatusEnum.SHIPPED;
			}
			if (wbStatus == WildberriesWbStatusEnum.SORTED
					|| wbStatus == WildberriesWbStatusEnum.READY_FOR_PICKUP
					|| wbStatus == WildberriesWbStatusEnum.POSTPONED_DELIVERY) {
				return ErpOrderStatusEnum.ARRIVED_AT_PLATFORM_WAREHOUSE;
			}
		}

		// 4️⃣ 已确认，待发货
		if (supplierStatus == WildberriesSupplierStatusEnum.CONFIRM) {
			return ErpOrderStatusEnum.READY_TO_SHIP;
		}

		// 5️⃣ 新建待处理
		if (supplierStatus == WildberriesSupplierStatusEnum.NEW
				|| wbStatus == WildberriesWbStatusEnum.WAITING) {
			return ErpOrderStatusEnum.READY_TO_SHIP;
		}

		// 6️⃣ 默认兜底
		log.warn("Unknown WB order status combination: supplierStatus={}, wbStatus={}", supplier, wb);
		return ErpOrderStatusEnum.READY_TO_SHIP;
	}

	/**
	 * 支持字符串输入的重载方法（外部接口调用方便）
	 */
	public static String toErpStatus(String supplierStatus, String wbStatus) {
		WildberriesSupplierStatusEnum supplierEnum = WildberriesSupplierStatusEnum.fromCode(supplierStatus);
		WildberriesWbStatusEnum wbEnum = WildberriesWbStatusEnum.fromCode(wbStatus);
		return toErpStatus(supplierEnum, wbEnum).name();
	}
}

