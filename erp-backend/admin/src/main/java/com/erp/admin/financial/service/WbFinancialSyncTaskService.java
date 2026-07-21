package com.erp.admin.financial.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.financial.mapper.WbFinancialSyncTaskMapper;
import com.erp.admin.financial.model.entity.WbFinancialSyncTask;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * WB 财务同步店铺任务服务
 *
 * @author system
 */
@Service
public class WbFinancialSyncTaskService extends ExtendServiceImpl<WbFinancialSyncTaskMapper, WbFinancialSyncTask> {

    /**
     * 查询待执行的任务
     * <p>
     * 查询条件：
     * 1. status = PENDING
     * 2. next_execute_time <= 当前时间
     * 3. 按 next_execute_time 升序排序
     * 4. 限制返回数量
     *
     * @param limit 最大返回数量
     * @return 待执行任务列表
     */
    public List<WbFinancialSyncTask> listPendingTasks(int limit) {
        // C2：调度器无租户上下文跨全部租户拉待执行任务，是合法平台级扫描；显式放行租户注入，
        // 否则 fail-closed 后 wb_financial_sync_task 被注入 -999 → 0 行、调度器空转。
        return TenantContext.runAsPlatformScan(() -> baseMapper.selectList(Wrappers.<WbFinancialSyncTask>lambdaQuery()
                .eq(WbFinancialSyncTask::getStatus, "PENDING")
                .le(WbFinancialSyncTask::getNextExecuteTime, LocalDateTime.now())
                .orderByAsc(WbFinancialSyncTask::getNextExecuteTime)
                .last("LIMIT " + limit)));
    }

    /**
     * 查询作业下的所有任务
     *
     * @param jobId 作业ID
     * @return 任务列表
     */
    public List<WbFinancialSyncTask> listByJobId(Long jobId) {
        return baseMapper.selectList(Wrappers.<WbFinancialSyncTask>lambdaQuery()
                .eq(WbFinancialSyncTask::getJobId, jobId)
                .orderByAsc(WbFinancialSyncTask::getCreateTime));
    }

    /**
     * 统计作业下各状态的任务数
     *
     * @param jobId  作业ID
     * @param status 任务状态
     * @return 任务数量
     */
    public long countByJobIdAndStatus(Long jobId, String status) {
        return baseMapper.selectCount(Wrappers.<WbFinancialSyncTask>lambdaQuery()
                .eq(WbFinancialSyncTask::getJobId, jobId)
                .eq(WbFinancialSyncTask::getStatus, status));
    }

    /**
     * 查询店铺最后成功同步的日期
     *
     * @param shopId 店铺ID
     * @return 最后同步日期,如果没有则返回 null
     */
    public LocalDate getLastSuccessDate(Long shopId) {
        WbFinancialSyncTask task = baseMapper.selectOne(Wrappers.<WbFinancialSyncTask>lambdaQuery()
                .eq(WbFinancialSyncTask::getShopId, shopId)
                .eq(WbFinancialSyncTask::getStatus, "COMPLETED")
                .orderByDesc(WbFinancialSyncTask::getDateTo, WbFinancialSyncTask::getId)
                .last("LIMIT 1"));
        
        return task != null ? task.getDateTo() : null;
    }

    /**
     * 尝试将任务状态更新为 RUNNING (使用乐观锁避免重复执行)
     * <p>
     * 使用 CAS (Compare-And-Set) 模式：
     * UPDATE ... WHERE id = ? AND status = 'PENDING'
     * <p>
     * 只有状态为 PENDING 的任务才能更新成功，避免重复执行
     *
     * @param taskId 任务ID
     * @return true-更新成功, false-任务已被其他线程执行
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean tryUpdateToRunning(Long taskId) {
        int updated = baseMapper.update(null, Wrappers.<WbFinancialSyncTask>lambdaUpdate()
                .set(WbFinancialSyncTask::getStatus, "RUNNING")
                .set(WbFinancialSyncTask::getUpdateTime, LocalDateTime.now())
                .eq(WbFinancialSyncTask::getId, taskId)
                .eq(WbFinancialSyncTask::getStatus, "PENDING")); // 只更新 PENDING 状态的任务
        
        return updated > 0;
    }

    /**
     * 更新任务为下次待执行状态
     *
     * @param taskId         任务ID
     * @param newRrdId       新的rrd_id
     * @param recordCount    本次记录数
     * @param nextExecuteTime 下次执行时间
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateForNextPage(Long taskId, Long newRrdId, Integer recordCount, LocalDateTime nextExecuteTime) {
        WbFinancialSyncTask task = this.getById(taskId);
        if (task != null) {
            task.setCurrentRrdId(newRrdId);
            task.setCompletedPages(task.getCompletedPages() + 1);
            task.setTotalRecords(task.getTotalRecords() + recordCount);
            task.setStatus("PENDING");
            task.setNextExecuteTime(nextExecuteTime);
            task.setUpdateTime(LocalDateTime.now());
            this.updateById(task);
        }
    }

    /**
     * 更新任务为完成状态
     *
     * @param taskId 任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void markCompleted(Long taskId) {
        WbFinancialSyncTask task = this.getById(taskId);
        if (task != null) {
            task.setStatus("COMPLETED");
            task.setNextExecuteTime(null);
            task.setUpdateTime(LocalDateTime.now());
            this.updateById(task);
        }
    }

    /**
     * 更新任务为失败状态
     *
     * @param taskId       任务ID
     * @param errorMessage 错误信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(Long taskId, String errorMessage) {
        WbFinancialSyncTask task = this.getById(taskId);
        if (task != null) {
            task.setStatus("FAILED");
            task.setErrorMessage(errorMessage);
            task.setRetryCount(task.getRetryCount() + 1);
            task.setUpdateTime(LocalDateTime.now());
            this.updateById(task);
        }
    }
}
