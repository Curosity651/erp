package com.erp.admin.order.service;

import com.erp.admin.order.model.entity.ErpOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单生命周期编排层
 * <p>
 * 统一触发订单状态变更的副作用（预占/释放）。
 * <p>
 * <b>异常语义</b>：本类不 catch 任何异常，调用方事务会因异常回滚。
 *
 * @see OrderReservationService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLifecycleService {

	private final OrderReservationService reservationService;

	/**
	 * 新订单入库后调用（同一事务内）
	 */
	public void onOrderCreated(ErpOrder order) {
		reservationService.reserveIfNeeded(order);
	}

	/**
	 * 订单状态变更后调用（同一事务内）
	 */
	public void onStatusChanged(ErpOrder order, String oldStatus, String newStatus) {
		if (oldStatus == null || oldStatus.equals(newStatus)) {
			return;
		}
		reservationService.handleStatusChange(order, oldStatus, newStatus);
	}

}
