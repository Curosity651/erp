package com.erp.admin.financial.service;

import com.erp.admin.financial.model.dto.SyncTaskResponse;
import com.erp.admin.financial.model.dto.TaskProgressResponse;
import com.erp.admin.financial.model.entity.WbFinancialSyncJob;
import com.erp.admin.financial.model.entity.WbFinancialSyncTask;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Wildberries 财务报表同步服务
 * <p>
 * 职责：
 * 1. 创建同步作业(Job)和任务(Task)
 * 2. 管理同步任务的生命周期
 * 3. 查询任务进度和状态
 * <p>
 * 注意：
 * - 不再直接调用 API，由 WbFinancialSyncTaskExecutor 异步执行
 * - 不再串行处理店铺，由 WbFinancialSyncScheduler 统一调度
 *
 * @author system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WbFinancialReportSyncService {

    private final ShopService shopService;
    private final WbFinancialSyncJobService jobService;
    private final WbFinancialSyncTaskService taskService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * 统一入口：按时间范围同步所有 WB 店铺的财务报表数据
     * <p>
     * 说明：
     * - 不再区分“全量/增量”接口，前端只按时间范围发起同步
     * - 如果未传入日期，则默认同步最近一年的数据
     * - shopIds 为空表示所有启用的 WB 店铺
     * - periodType 支持 weekly(周报)/daily(日报)，默认 weekly
     */
    @Transactional(rollbackFor = Exception.class)
    public SyncTaskResponse syncByDateRange(String dateFrom, String dateTo, List<Long> shopIds, String periodType) {
        LocalDateTime now = LocalDateTime.now();

        // 处理默认周期类型
        String finalPeriodType = StringUtils.hasText(periodType) ? periodType : "weekly";
        if (!"weekly".equals(finalPeriodType) && !"daily".equals(finalPeriodType)) {
            throw new IllegalArgumentException("不支持的周期类型: " + periodType + ", 仅支持 weekly/daily");
        }

        // 处理默认日期：都为空则默认最近一年，只传一端则补齐另一端
        LocalDate today = LocalDate.now();
        LocalDate from;
        LocalDate to;

        if (!StringUtils.hasText(dateFrom) && !StringUtils.hasText(dateTo)) {
            to = today;
            from = today.minusYears(1);
        }
        else if (StringUtils.hasText(dateFrom) && !StringUtils.hasText(dateTo)) {
            from = LocalDate.parse(dateFrom, DATE_FORMATTER);
            to = today;
        }
        else if (!StringUtils.hasText(dateFrom) && StringUtils.hasText(dateTo)) {
            to = LocalDate.parse(dateTo, DATE_FORMATTER);
            from = to.minusYears(1);
        }
        else {
            // 两端都有则直接使用，并做合法性校验
            validateDateRange(dateFrom, dateTo);
            from = LocalDate.parse(dateFrom, DATE_FORMATTER);
            to = LocalDate.parse(dateTo, DATE_FORMATTER);
        }

        String fromStr = from.format(DATE_FORMATTER);
        String toStr = to.format(DATE_FORMATTER);
        log.info("[范围同步] 最终日期范围={} ~ {}, 周期类型={}", fromStr, toStr, finalPeriodType);

        // 1. 创建同步作业
        String jobCode = "RANGE-" + UUID.randomUUID().toString().substring(0, 8);
        WbFinancialSyncJob job = new WbFinancialSyncJob();
        job.setJobCode(jobCode);
        job.setSyncType("RANGE");
        job.setPeriodType(finalPeriodType);
        job.setDateFrom(from);
        job.setDateTo(to);
        job.setStatus("RUNNING");
        job.setTotalShops(0);
        job.setCompletedShops(0);
        job.setFailedShops(0);
        job.setTotalRecords(0);
        job.setCreateTime(now);
        job.setUpdateTime(now);
        jobService.save(job);

        log.info("[作业创建] jobId={}, jobCode={}", job.getId(), jobCode);

        // 2. 查询所有启用的 WB 店铺
        List<Shop> shops = shopService.listEnabledByPlatform(PlatformEnum.Wildberries.code());

        // 如果传入了 shopIds 进行过滤
        if (shopIds != null && !shopIds.isEmpty()) {
            Set<Long> filterSet = new HashSet<>(shopIds);
            shops = shops.stream().filter(s -> filterSet.contains(s.getId())).collect(Collectors.toList());
            log.info("[范围同步] 过滤店铺，指定 shopIds={}, 实际可同步店铺数={}", shopIds, shops.size());
        }

        if (shops.isEmpty()) {
            log.warn("[范围同步] 没有启用的 Wildberries 店铺");
            job.setStatus("COMPLETED");
            job.setUpdateTime(LocalDateTime.now());
            jobService.updateById(job);

            return SyncTaskResponse.builder()
                    .jobId(job.getId())
                    .jobCode(jobCode)
                    .syncType("RANGE")
                    .totalShops(0)
                    .taskIds(new ArrayList<>())
                    .createTime(now)
                    .message("没有启用的 Wildberries 店铺")
                    .build();
        }

        // 3. 为每个店铺创建任务（同一日期范围）
        List<Long> taskIds = new ArrayList<>();
        for (Shop shop : shops) {
            WbFinancialSyncTask task = new WbFinancialSyncTask();
            task.setJobId(job.getId());
            task.setShopId(shop.getId());
            task.setShopName(shop.getName());
            task.setDateFrom(from);
            task.setDateTo(to);
            task.setPeriodType(finalPeriodType);
            task.setCurrentRrdId(0L);
            task.setTotalPages(0);
            task.setCompletedPages(0);
            task.setTotalRecords(0);
            task.setStatus("PENDING");
            task.setNextExecuteTime(now);
            task.setRetryCount(0);
            task.setCreateTime(now);
            task.setUpdateTime(now);

            taskService.save(task);
            taskIds.add(task.getId());

            log.info("[任务创建] taskId={}, shopId={}, shopName={}", task.getId(), shop.getId(), shop.getName());
        }

        // 4. 更新作业的店铺总数
        job.setTotalShops(shops.size());
        job.setUpdateTime(LocalDateTime.now());
        jobService.updateById(job);

        log.info("[范围同步] 作业创建完成, jobId={}, 待同步店铺数={}", job.getId(), shops.size());

        return SyncTaskResponse.builder()
                .jobId(job.getId())
                .jobCode(jobCode)
                .syncType("RANGE")
                .totalShops(shops.size())
                .taskIds(taskIds)
                .createTime(now)
                .message(String.format("已创建 %d 个店铺同步任务，调度器将自动执行", shops.size()))
                .build();
    }

    /**
     * 查询任务进度
     *
     * @param jobId 作业ID
     * @return 任务进度列表
     */
    public List<TaskProgressResponse> getTaskProgress(Long jobId) {
        List<WbFinancialSyncTask> tasks = taskService.listByJobId(jobId);

        return tasks.stream().map(task -> {
            // 计算进度百分比(简单估算)
            int progressPercent = 0;
            if ("COMPLETED".equals(task.getStatus())) {
                progressPercent = 100;
            } else if ("RUNNING".equals(task.getStatus()) || "PENDING".equals(task.getStatus())) {
                // 简单估算: 假设平均50页
                progressPercent = Math.min(95, task.getCompletedPages() * 2);
            }

            return TaskProgressResponse.builder()
                    .taskId(task.getId())
                    .jobId(task.getJobId())
                    .shopId(task.getShopId())
                    .shopName(task.getShopName())
                    .status(task.getStatus())
                    .completedPages(task.getCompletedPages())
                    .totalPages(task.getTotalPages())
                    .totalRecords(task.getTotalRecords())
                    .progressPercent(progressPercent)
                    .nextExecuteTime(task.getNextExecuteTime())
                    .errorMessage(task.getErrorMessage())
                    .createTime(task.getCreateTime())
                    .updateTime(task.getUpdateTime())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 查询作业信息
     *
     * @param jobId 作业ID
     * @return 作业信息
     */
    public WbFinancialSyncJob getJob(Long jobId) {
        return jobService.getById(jobId);
    }

    /**
     * 校验日期范围参数
     * <p>
     * 校验规则：
     * 1. dateFrom 和 dateTo 不能为空
     * 2. dateFrom 不能晚于 dateTo
     *
     * @param dateFrom 开始日期
     * @param dateTo   结束日期
     * @throws IllegalArgumentException 如果参数无效
     */
    private void validateDateRange(String dateFrom, String dateTo) {
        // 校验非空
        if (!StringUtils.hasText(dateFrom)) {
            throw new IllegalArgumentException("dateFrom 不能为空");
        }
        if (!StringUtils.hasText(dateTo)) {
            throw new IllegalArgumentException("dateTo 不能为空");
        }

        // 校验日期顺序
        try {
            LocalDate from = LocalDate.parse(dateFrom, DATE_FORMATTER);
            LocalDate to = LocalDate.parse(dateTo, DATE_FORMATTER);

            if (from.isAfter(to)) {
                throw new IllegalArgumentException(
                        String.format("dateFrom (%s) 不能晚于 dateTo (%s)", dateFrom, dateTo));
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalArgumentException(
                    String.format("日期格式错误，请使用 yyyy-MM-dd 格式。dateFrom=%s, dateTo=%s",
                            dateFrom, dateTo), e);
        }
    }
}
