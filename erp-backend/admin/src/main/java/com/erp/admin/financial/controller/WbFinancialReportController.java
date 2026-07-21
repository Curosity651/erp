package com.erp.admin.financial.controller;

import java.util.List;

import com.erp.admin.financial.converter.WbReportDetailConverter;
import com.erp.admin.financial.model.entity.WbReportDetail;
import com.erp.admin.financial.model.vo.WbReportDetailExportVO;
import com.erp.admin.financial.model.qo.WbReportDetailQO;
import com.erp.admin.financial.model.vo.WbReportDetailPageVO;
import com.erp.admin.financial.service.WbFinancialReportSyncService;
import com.erp.admin.financial.service.WbReportDetailService;
import com.erp.admin.financial.model.dto.FullSyncRequest;
import com.erp.admin.financial.model.dto.SyncTaskResponse;
import com.erp.admin.financial.model.dto.TaskProgressResponse;
import com.erp.admin.financial.model.entity.WbFinancialSyncJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.fastexcel.annotation.ResponseExcel;
import org.ballcat.web.accesslog.annotation.AccessLoggingRule;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wildberries 财务报表查询 Controller
 * <p>
 * 提供财务报表数据查询和导出接口
 *
 * @author system
 */
@RestController
@RequestMapping("/financial/wb-report")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wildberries 财务报表")
public class WbFinancialReportController {

    private final WbReportDetailService reportDetailService;
    private final WbFinancialReportSyncService syncService;

    /**
     * 分页查询财务报表明细
     *
     * @param pageParam 分页参数
     * @param qo        查询条件
     * @return 分页结果
     */
	@PreAuthorize("@per.hasPermission('financial:wb-report:read')")
	@GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询 Wildberries 财务报表明细数据")
    public ApiResult<PageResult<WbReportDetailPageVO>> page(PageParam pageParam, WbReportDetailQO qo) {
        try {
            log.info("[API] 收到财务报表分页查询请求: page={}, size={}", pageParam.getPage(), pageParam.getSize());

            PageResult<WbReportDetailPageVO> result = reportDetailService.queryPage(pageParam, qo);

            log.info("[API] 查询成功: total={}, records={}", result.getTotal(), result.getRecords().size());

            return ApiResult.ok(result);

        } catch (Exception e) {
            log.error("[API] 分页查询失败: {}", e.getMessage(), e);
            return ApiResult.failed(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 导出财务报表明细
     *
     * @param qo 查询条件
     * @return Excel 文件
     */
	@PreAuthorize("@per.hasPermission('financial:wb-report:export')")
	@PostMapping("/export")
	@AccessLoggingRule(includeQueryString = true, includeRequestBody = true)
	@Operation(summary = "导出数据", description = "根据查询条件导出 Wildberries 财务报表数据为 Excel 文件")
    @ResponseExcel(name = "WB财务报表_#{currentDateTime()}")
    public List<WbReportDetailExportVO> export(@RequestBody WbReportDetailQO qo) {
        log.info("[API] 收到财务报表导出请求");
        List<WbReportDetail> list = reportDetailService.listForExport(qo);
        return WbReportDetailConverter.INSTANCE.toExportVOList(list);
    }

    /**
     * 按时间范围同步（统一入口）
     * <p>
     * 说明：
     * - 不再区分全量/增量接口，前端只传时间范围和可选店铺列表
     * - 如果未传日期，后端默认使用最近一年的范围
     * - periodType 支持 weekly(周报)/daily(日报)，默认 weekly
     */
	@PreAuthorize("@per.hasPermission('financial:wb-report:sync')")
	@PostMapping("/sync")
    @Operation(summary = "按时间范围同步", description = "根据时间范围、店铺列表和周期类型创建同步任务，任务由调度器异步执行")
    public ApiResult<SyncTaskResponse> syncByDateRange(@RequestBody FullSyncRequest request) {
        try {
        log.info("[API] 收到范围同步请求: dateFrom={}, dateTo={}, shopIds={}, periodType={}",
            request.getDateFrom(), request.getDateTo(), request.getShopIds(), request.getPeriodType());

        SyncTaskResponse response = syncService.syncByDateRange(
            request.getDateFrom(), request.getDateTo(), request.getShopIds(), request.getPeriodType());

            log.info("[API] 范围同步任务创建成功: jobId={}, taskCount={}",
                    response.getJobId(), response.getTotalShops());

            return ApiResult.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("[API] 参数校验失败: {}", e.getMessage());
            return ApiResult.failed(400, e.getMessage());

        } catch (Exception e) {
            log.error("[API] 范围同步失败: {}", e.getMessage(), e);
            return ApiResult.failed(500, "范围同步失败: " + e.getMessage());
        }
    }

    /**
     * 查询作业信息
     */
	@GetMapping("/sync/job/{jobId}")
    @Operation(summary = "查询作业信息", description = "根据作业ID查询同步作业的详细信息")
    public ApiResult<WbFinancialSyncJob> getJob(@PathVariable Long jobId) {
        try {
            WbFinancialSyncJob job = syncService.getJob(jobId);
            if (job == null) {
                return ApiResult.failed(404, "作业不存在");
            }
            return ApiResult.ok(job);

        } catch (Exception e) {
            log.error("[API] 查询作业信息失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return ApiResult.failed(500, "查询作业信息失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务进度
     */
	@GetMapping("/sync/job/{jobId}/tasks")
    @Operation(summary = "查询任务进度", description = "查询指定作业下所有任务的执行进度")
    public ApiResult<List<TaskProgressResponse>> getTaskProgress(@PathVariable Long jobId) {
        try {
            List<TaskProgressResponse> progress = syncService.getTaskProgress(jobId);
            return ApiResult.ok(progress);

        } catch (Exception e) {
            log.error("[API] 查询任务进度失败: jobId={}, error={}", jobId, e.getMessage(), e);
            return ApiResult.failed(500, "查询任务进度失败: " + e.getMessage());
        }
    }
}
