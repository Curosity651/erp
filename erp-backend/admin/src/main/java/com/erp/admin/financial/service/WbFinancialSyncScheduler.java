package com.erp.admin.financial.service;

import com.erp.admin.financial.model.entity.WbFinancialSyncTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WB 财务同步任务调度器
 * <p>
 * 职责：
 * 1. 定时扫描待执行的任务
 * 2. 提交任务到异步线程池执行
 * 3. 控制并发数量
 * <p>
 * 调度策略：
 * - 每 10 秒扫描一次
 * - 每次最多提交 20 个任务
 * - 查询条件: status=PENDING 且 next_execute_time <= now
 *
 * @author system
 */
@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "financial.sync.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class WbFinancialSyncScheduler {

    private final WbFinancialSyncTaskService taskService;
    private final WbFinancialSyncTaskExecutor taskExecutor;

    /**
     * 每 10 秒扫描并调度待执行任务
     * <p>
     * 执行流程：
     * 1. 查询待执行的任务(最多20个)
     * 2. 遍历任务列表
     * 3. 提交到异步线程池执行
     * <p>
     * 注意：任务执行器中使用 CAS 机制防止重复执行
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void scheduleTasks() {
        try {
            // 查询待执行任务(最多20个)
            List<WbFinancialSyncTask> pendingTasks = taskService.listPendingTasks(20);

            if (pendingTasks.isEmpty()) {
                return;
            }

            log.info("[任务调度] 发现 {} 个待执行任务", pendingTasks.size());

            int submitCount = 0;
            // 提交到异步线程池执行
            for (WbFinancialSyncTask task : pendingTasks) {
                try {
                    // 异步执行，内部会使用 CAS 机制避免重复执行
                    taskExecutor.executeTask(task);
                    submitCount++;
                    
                    log.debug("[任务提交] taskId={}, shopId={}, shopName={}, rrdId={}",
                            task.getId(), task.getShopId(), task.getShopName(), task.getCurrentRrdId());
                    
                } catch (Exception e) {
                    log.error("[任务提交失败] taskId={}, shopName={}, 错误={}",
                            task.getId(), task.getShopName(), e.getMessage(), e);
                }
            }

            log.info("[任务调度] 已提交 {} 个任务到执行队列", submitCount);

        } catch (Exception e) {
            log.error("[任务调度异常] 错误={}", e.getMessage(), e);
        }
    }

    /**
     * 每小时统计一次任务执行情况
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void reportStatistics() {
        try {
            // TODO: 可以在这里添加任务执行统计报告
            log.info("[任务统计] 定时统计任务执行情况");
            
        } catch (Exception e) {
            log.error("[任务统计异常] 错误={}", e.getMessage(), e);
        }
    }
}
