package com.erp.admin.financial.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.financial.mapper.WbFinancialSyncJobMapper;
import com.erp.admin.financial.model.entity.WbFinancialSyncJob;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WB 财务同步作业服务
 *
 * @author system
 */
@Service
public class WbFinancialSyncJobService extends ExtendServiceImpl<WbFinancialSyncJobMapper, WbFinancialSyncJob> {

    /**
     * 根据作业编号查询
     *
     * @param jobCode 作业编号
     * @return 作业信息
     */
    public WbFinancialSyncJob getByJobCode(String jobCode) {
        return baseMapper.selectOne(Wrappers.<WbFinancialSyncJob>lambdaQuery()
                .eq(WbFinancialSyncJob::getJobCode, jobCode));
    }

    /**
     * 查询运行中的作业列表
     *
     * @return 运行中的作业列表
     */
    public List<WbFinancialSyncJob> listRunning() {
        return baseMapper.selectList(Wrappers.<WbFinancialSyncJob>lambdaQuery()
                .eq(WbFinancialSyncJob::getStatus, "RUNNING")
                .orderByDesc(WbFinancialSyncJob::getId));
    }

    /**
     * 更新作业统计信息
     *
     * @param jobId           作业ID
     * @param completedShops  已完成店铺数
     * @param failedShops     失败店铺数
     * @param totalRecords    总记录数
     */
    public void updateStatistics(Long jobId, Integer completedShops, Integer failedShops, Integer totalRecords) {
        WbFinancialSyncJob job = this.getById(jobId);
        if (job != null) {
            job.setCompletedShops(completedShops);
            job.setFailedShops(failedShops);
            job.setTotalRecords(totalRecords);
            
            // 如果所有店铺都处理完成,更新作业状态
            if (completedShops + failedShops >= job.getTotalShops()) {
                job.setStatus(failedShops > 0 ? "FAILED" : "COMPLETED");
            }
            
            this.updateById(job);
        }
    }
}
