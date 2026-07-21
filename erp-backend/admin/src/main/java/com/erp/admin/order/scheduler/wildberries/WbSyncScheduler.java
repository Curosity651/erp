package com.erp.admin.order.scheduler.wildberries;

import com.erp.admin.order.service.wildberries.WbOrderSyncService;
import com.erp.admin.order.service.wildberries.sync.WbOfficeSyncService;
import com.erp.admin.order.service.wildberries.sync.WbOrderStatusSyncService;
import com.erp.admin.order.service.wildberries.sync.WbSupplySyncService;
import com.erp.admin.platform.wildberries.config.WildberriesSyncProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * WbSyncScheduler - Wildberries 统一定时任务调度器
 * <p>
 * 职责：
 * - 整合所有同步的定时触发
 * - 只负责定时触发，不包含业务逻辑
 * - 委托 WbSyncService 执行实际同步
 * <p>
 * 注意：定时任务与业务逻辑分离
 *
 * @author system
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WbSyncScheduler {

	private final WbOrderSyncService wbOrderSyncService;
	private final WbOrderStatusSyncService wbOrderStatusSyncService;
	private final WbSupplySyncService wbSupplySyncService;
	private final WbOfficeSyncService wbOfficeSyncService;
	private final WildberriesSyncProperties wildberriesSyncProperties;

	/**
	 * 定时同步订单（增量）
	 * 默认每 30 分钟执行一次
	 */
	@Scheduled(cron = "${wildberries.sync.order.cron:0 0/30 * * * *}")
	public void scheduledSyncOrders() {
		if (!wildberriesSyncProperties.isEnabled()) {
			log.debug("[WB][SCHEDULER] 订单同步已禁用");
			return;
		}

		log.info("[WB][SCHEDULER] 开始定时同步订单（增量）");
		try {
			wbOrderSyncService.syncAllEnabledShopOrders(false);
			log.info("[WB][SCHEDULER] 订单同步完成");
		} catch (Exception e) {
			log.error("[WB][SCHEDULER] 订单同步失败", e);
		}
	}

	/**
	 * 定时同步未完结订单状态
	 * 默认每 30 分钟执行一次
	 */
	@Scheduled(cron = "${wildberries.sync.order-status.cron:0 5/30 * * * *}")
	public void scheduledSyncUnfinishedOrderStatuses() {
		if (!wildberriesSyncProperties.isEnabled()) {
			log.debug("[WB][SCHEDULER] 订单状态同步已禁用");
			return;
		}

		log.info("[WB][SCHEDULER] 开始定时同步未完结订单状态");
		try {
			wbOrderStatusSyncService.syncAllEnabledShopsUnfinishedOrderStatuses();
			log.info("[WB][SCHEDULER] 未完结订单状态同步完成");
		} catch (Exception e) {
			log.error("[WB][SCHEDULER] 未完结订单状态同步失败", e);
		}
	}

	/**
	 * 定时同步 Supply
	 * 默认每 6 小时执行一次
	 */
	@Scheduled(cron = "${wildberries.sync.supply.cron:0 0 */6 * * *}")
	public void scheduledSyncSupplies() {
		if (!wildberriesSyncProperties.isEnabled()) {
			log.debug("[WB][SCHEDULER] Supply 同步已禁用");
			return;
		}

		log.info("[WB][SCHEDULER] 开始定时同步 Supply");
		try {
			wbSupplySyncService.syncAllEnabledShopsSupplies();
			log.info("[WB][SCHEDULER] Supply 同步完成");
		} catch (Exception e) {
			log.error("[WB][SCHEDULER] Supply 同步失败", e);
		}
	}

	/**
	 * 定时同步 Office
	 * 默认每天凌晨 2 点执行一次
	 */
	@Scheduled(cron = "${wildberries.sync.office.cron:0 0 2 * * *}")
	public void scheduledSyncOffices() {
		if (!wildberriesSyncProperties.isEnabled()) {
			log.debug("[WB][SCHEDULER] Office 同步已禁用");
			return;
		}

		log.info("[WB][SCHEDULER] 开始定时同步 Office");
		try {
			wbOfficeSyncService.syncAllEnabledShopsOffices();
			log.info("[WB][SCHEDULER] Office 同步完成");
		} catch (Exception e) {
			log.error("[WB][SCHEDULER] Office 同步失败", e);
		}
	}
}
