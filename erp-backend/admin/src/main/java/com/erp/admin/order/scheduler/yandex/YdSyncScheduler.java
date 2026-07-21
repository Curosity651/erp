package com.erp.admin.order.scheduler.yandex;

import com.erp.admin.order.service.yandex.YdOrderSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Yandex 定时同步调度器
 * <p>
 * cron 时间安排（与 WB/Ozon 错开）：
 * - WB:     :00/:30（增量） :05/:35（状态回溯）
 * - Ozon:   :10/:40（增量） :15/:45（状态回溯）
 * - Yandex: :20/:50（增量） :25/:55（状态回溯）
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yandex.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class YdSyncScheduler {

    private final YdOrderSyncService ydOrderSyncService;

    /**
     * 增量同步订单，每 30 分钟
     */
    @Scheduled(cron = "${yandex.sync.order.cron:0 20/30 * * * *}")
    public void syncOrders() {
        log.info("[YANDEX][SCHEDULER] 开始定时同步订单（增量）");
        try {
            ydOrderSyncService.syncAllShops(false);
            log.info("[YANDEX][SCHEDULER] 订单同步完成");
        } catch (Exception e) {
            log.error("[YANDEX][SCHEDULER] 订单同步失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 未完结订单状态回溯，每 30 分钟（错开 5 分钟）
     */
    @Scheduled(cron = "${yandex.sync.order-status.cron:0 25/30 * * * *}")
    public void syncUnfinishedOrders() {
        log.info("[YANDEX][SCHEDULER] 开始定时同步未完结订单状态");
        try {
            ydOrderSyncService.syncUnfinishedOrders();
            log.info("[YANDEX][SCHEDULER] 未完结订单状态同步完成");
        } catch (Exception e) {
            log.error("[YANDEX][SCHEDULER] 未完结订单状态同步失败: {}", e.getMessage(), e);
        }
    }
}
