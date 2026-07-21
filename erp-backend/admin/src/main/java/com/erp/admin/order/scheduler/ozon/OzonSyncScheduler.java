package com.erp.admin.order.scheduler.ozon;

import com.erp.admin.order.service.ozon.OzonOrderSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * OzonSyncScheduler - Ozon 定时任务调度器
 * <p>
 * 职责：
 * - 定时触发订单同步
 * - 只负责定时触发，不包含业务逻辑
 * - 委托 OzonOrderSyncService 执行实际同步
 * <p>
 * 注意：
 * - 定时任务与业务逻辑分离
 * - 可通过配置启用/禁用
 * - 异常不影响下次执行
 *
 * @author system
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "ozon.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OzonSyncScheduler {

    private final OzonOrderSyncService ozonOrderSyncService;

    /**
     * 定时同步订单（FBS + FBO）- 增量同步
     * <p>
     * 默认每 30 分钟执行一次
     * <p>
     * 可通过配置修改：ozon.sync.order.cron
     */
    @Scheduled(cron = "${ozon.sync.order.cron:0 10/30 * * * *}")
    public void syncOrders() {
        log.info("[OZON][SCHEDULER] 开始定时同步订单（增量）");

        try {
            ozonOrderSyncService.syncAllShops(false);
            log.info("[OZON][SCHEDULER] 订单同步完成");
        } catch (Exception e) {
            log.error("[OZON][SCHEDULER] 订单同步失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响下次执行
        }
    }

    /**
     * 定时同步未完结订单状态
     * <p>
     * 默认每 30 分钟执行一次
     * <p>
     * 可通过配置修改：ozon.sync.order-status.cron
     */
    @Scheduled(cron = "${ozon.sync.order-status.cron:0 15/30 * * * *}")
    public void syncUnfinishedOrderStatuses() {
        log.info("[OZON][SCHEDULER] 开始定时同步未完结订单状态");

        try {
            ozonOrderSyncService.syncAllEnabledShopsUnfinishedOrderStatuses();
            log.info("[OZON][SCHEDULER] 未完结订单状态同步完成");
        } catch (Exception e) {
            log.error("[OZON][SCHEDULER] 未完结订单状态同步失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响下次执行
        }
    }
}
