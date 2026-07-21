package com.erp.admin.financial.service;

import com.erp.admin.financial.model.entity.WbFinancialSyncPageLog;
import com.erp.admin.financial.model.entity.WbFinancialSyncTask;
import com.erp.admin.financial.model.entity.WbReportDetail;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.wildberries.credential.WbCredential;
import com.erp.admin.platform.wildberries.WildberriesClient;
import com.erp.admin.platform.wildberries.model.request.report.WbReportDetailRequest;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WB 财务同步任务执行器
 * <p>
 * 职责：
 * 1. 异步执行单个店铺的单次分页拉取
 * 2. 调用 WB API 获取数据
 * 3. 保存数据到数据库
 * 4. 更新任务状态
 * 5. 记录执行日志
 *
 * @author system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WbFinancialSyncTaskExecutor {

    private final WildberriesClient wildberriesClient;
    private final ShopService shopService;
    private final WbReportDetailService wbReportDetailService;
    private final WbFinancialSyncTaskService taskService;
    private final WbFinancialSyncPageLogService pageLogService;
    private final WbFinancialSyncJobService jobService;
	private final CredentialService credentialService;

    /**
     * 异步执行单个任务
     * <p>
     * 执行流程：
     * 1. 更新任务状态为 RUNNING (独立事务)
     * 2. 获取店铺凭证
     * 3. 调用 API 拉取数据(受限流控制，无事务)
     * 4. 保存数据到数据库 (独立事务)
     * 5. 记录执行日志 (独立事务)
     * 6. 更新任务状态 (独立事务):
     *    - 如果返回空数组: COMPLETED
     *    - 否则: PENDING + 设置下次执行时间
     * <p>
     * 注意：移除方法级 @Transactional，避免长时间持有数据库锁
     *
     * @param task 待执行的任务
     */
    @Async("financialSyncExecutor")
    public void executeTask(WbFinancialSyncTask task) {
        long startTime = System.currentTimeMillis();
        
        log.info("[任务执行开始] taskId={}, shopId={}, shopName={}, page={}, rrdId={}",
                task.getId(), task.getShopId(), task.getShopName(),
                task.getCompletedPages() + 1, task.getCurrentRrdId());

        try {
            // 多租户：异步执行器无继承上下文，按任务所属租户建立，确保财务明细/任务更新落对货主
            TenantContext.setCurrentTenant(task.getTenantId());
            // 1. 更新任务状态为 RUNNING (独立事务，快速释放锁)
            boolean updated = taskService.tryUpdateToRunning(task.getId());
            if (!updated) {
                log.warn("[任务跳过] taskId={} 已被其他线程执行或状态已改变", task.getId());
                return;
            }

            // 2. 获取店铺信息和凭证
            Shop shop = shopService.getById(task.getShopId());
            if (shop == null) {
                throw new IllegalStateException("店铺不存在: " + task.getShopId());
            }
            WbCredential credential = credentialService.parseCredential(shop);

            // 3. 构建请求
            WbReportDetailRequest request = WbReportDetailRequest.builder()
                    .dateFrom(task.getDateFrom().toString())
                    .dateTo(task.getDateTo().toString())
                    .limit(100000)  // 单次最大值
                    .rrdid(task.getCurrentRrdId())
                    .period(task.getPeriodType())  // 传递周期类型
                    .build();

            // 4. 调用 API 拉取数据(限流器会自动阻塞，不在事务中)
            List<WbReportDetail> pageData = wildberriesClient.getReportDetailByPeriod(
                    credential, request);

            long duration = System.currentTimeMillis() - startTime;

            // 5. 处理返回数据 (各自使用独立事务)
            if (pageData == null || pageData.isEmpty()) {
                // 数据拉取完成
                handleTaskCompleted(task, duration);
            } else {
                // 保存数据并继续分页
                handlePageDataReceived(task, pageData, duration);
            }

            // 6. 更新作业统计
            updateJobStatistics(task.getJobId());

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            handleTaskError(task, e, duration);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 处理数据拉取完成
     *
     * @param task     任务
     * @param duration 耗时
     */
    private void handleTaskCompleted(WbFinancialSyncTask task, long duration) {
        log.info("[任务执行完成] taskId={}, shopName={}, 总页数={}, 总记录数={}, 耗时={}ms",
                task.getId(), task.getShopName(), task.getCompletedPages(),
                task.getTotalRecords(), duration);

        // 更新任务为完成状态
        taskService.markCompleted(task.getId());

        // 记录日志
        savePageLog(task, task.getCurrentRrdId(), 0, "SUCCESS", (int) duration, null);
    }

    /**
     * 处理接收到的分页数据
     *
     * @param task     任务
     * @param pageData 分页数据
     * @param duration 耗时
     */
    private void handlePageDataReceived(WbFinancialSyncTask task, List<WbReportDetail> pageData, long duration) {
        // 为每条记录设置 shopId 和时间戳（periodType 由 batchSaveOrUpdate 统一设置）
        LocalDateTime now = LocalDateTime.now();
        pageData.forEach(detail -> {
            detail.setShopId(task.getShopId());
            detail.setPeriodType(task.getPeriodType());
            detail.setCreateTime(now);
            detail.setUpdateTime(now);
        });

        // 保存数据到数据库（使用 periodType + rrdId 作为唯一键）
        int savedCount = wbReportDetailService.batchSaveOrUpdate(task.getPeriodType(), pageData);

        log.info("[数据保存成功] taskId={}, shopName={}, page={}, 本次记录数={}, 保存数={}, 耗时={}ms",
                task.getId(), task.getShopName(), task.getCompletedPages() + 1,
                pageData.size(), savedCount, duration);

        // 提取最后一条记录的 rrd_id 作为下次分页参数
        Long lastRrdId = pageData.get(pageData.size() - 1).getRrdId();

        // 更新任务状态: 继续分页
        LocalDateTime nextExecuteTime = LocalDateTime.now().plusMinutes(1); // 1分钟后执行
        taskService.updateForNextPage(task.getId(), lastRrdId, savedCount, nextExecuteTime);

        // 记录日志
        savePageLog(task, task.getCurrentRrdId(), savedCount, "SUCCESS", (int) duration, null);

        log.info("[任务更新] taskId={}, 下次执行时间={}, 下次rrdId={}",
                task.getId(), nextExecuteTime, lastRrdId);
    }

    /**
     * 处理任务错误
     *
     * @param task     任务
     * @param e        异常
     * @param duration 耗时
     */
    private void handleTaskError(WbFinancialSyncTask task, Exception e, long duration) {
        String errorMessage = e.getMessage();

        log.error("[任务执行失败] taskId={}, shopName={}, page={}, 错误={}, 耗时={}ms",
                task.getId(), task.getShopName(), task.getCompletedPages() + 1,
                errorMessage, duration, e);

        // 更新任务为失败状态
        taskService.markFailed(task.getId(), errorMessage);

        // 记录日志
        savePageLog(task, task.getCurrentRrdId(), 0, "FAILED", (int) duration, errorMessage);
    }

    /**
     * 保存分页执行日志
     */
    private void savePageLog(WbFinancialSyncTask task, Long rrdId, Integer recordCount,
                             String status, Integer durationMs, String errorMessage) {
        WbFinancialSyncPageLog log = new WbFinancialSyncPageLog();
        log.setTaskId(task.getId());
        log.setPageNum(task.getCompletedPages() + 1);
        log.setRrdId(rrdId);
        log.setRecordCount(recordCount);
        log.setStatus(status);
        log.setExecuteTime(LocalDateTime.now());
        log.setDurationMs(durationMs);
        log.setErrorMessage(errorMessage);
        log.setCreateTime(LocalDateTime.now());

        pageLogService.save(log);
    }

    /**
     * 更新作业统计信息
     */
    private void updateJobStatistics(Long jobId) {
        long completedCount = taskService.countByJobIdAndStatus(jobId, "COMPLETED");
        long failedCount = taskService.countByJobIdAndStatus(jobId, "FAILED");

        // 统计总记录数
        List<WbFinancialSyncTask> tasks = taskService.listByJobId(jobId);
        int totalRecords = tasks.stream()
                .mapToInt(t -> t.getTotalRecords() != null ? t.getTotalRecords() : 0)
                .sum();

        jobService.updateStatistics(jobId, (int) completedCount, (int) failedCount, totalRecords);
    }

}
