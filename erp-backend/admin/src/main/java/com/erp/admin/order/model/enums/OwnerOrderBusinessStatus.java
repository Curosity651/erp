package com.erp.admin.order.model.enums;

import com.erp.admin.order.model.entity.ErpOrder;

/** 面向 ERP 货主展示的简化订单履约状态。 */
public enum OwnerOrderBusinessStatus {
	PENDING_CONFIRM,
	WAITING_SHELF,
	OUTBOUND_PROCESSING,
	HANDED_OVER,
	CANCELLED,
	EXCEPTION;

	public static OwnerOrderBusinessStatus resolve(ErpOrder order) {
		String warehouseStatus = order.getWarehouseFulfillmentStatus();
		if (warehouseStatus == null || warehouseStatus.trim().isEmpty()) {
			String erpStatus = order.getErpStatus();
			if ("CANCELED".equals(erpStatus) || "CANCELLED".equals(erpStatus)
					|| "RETURNED".equals(erpStatus)) {
				return CANCELLED;
			}
			return PENDING_CONFIRM;
		}
		switch (warehouseStatus) {
			case "WAITING_SHELF":
				return WAITING_SHELF;
			case "PLATFORM_PROCESSING":
			case "WAITING_PICK":
			case "PICKING":
			case "WAITING_PACK":
			case "PACKED":
				return OUTBOUND_PROCESSING;
			case "SHIPPED":
				return HANDED_OVER;
			case "CANCEL_RETURNING":
			case "CANCELLED":
				return CANCELLED;
			case "EXCEPTION":
				return EXCEPTION;
			default:
				return PENDING_CONFIRM;
		}
	}
}
