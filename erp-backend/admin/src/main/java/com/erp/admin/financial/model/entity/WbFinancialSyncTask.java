package com.erp.admin.financial.model.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WB 财务同步店铺任务表
 * <p>
 * 每个店铺创建一个任务，支持分页断点续传
 *
 * @author system
 */
@TableName("wb_financial_sync_task")
@Data
@Schema(description = "WB财务同步店铺任务")
public class WbFinancialSyncTask {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 所属租户(只读)，供异步执行器 runAs；写入由多租户拦截器处理 */
    @TableField(value = "tenant_id", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @Schema(description = "所属租户(只读)")
    private Long tenantId;

    @Schema(description = "所属作业ID")
    private Long jobId;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "开始日期")
    private LocalDate dateFrom;

    @Schema(description = "结束日期")
    private LocalDate dateTo;

    @Schema(description = "报表周期类型：weekly-周报, daily-日报")
    private String periodType;

    @Schema(description = "当前分页游标(rrd_id)")
    private Long currentRrdId;

    @Schema(description = "总页数")
    private Integer totalPages;

    @Schema(description = "已完成页数")
    private Integer completedPages;

    @Schema(description = "已同步记录数")
    private Integer totalRecords;

    @Schema(description = "状态: PENDING-待执行/RUNNING-运行中/COMPLETED-已完成/FAILED-失败")
    private String status;

    @Schema(description = "下次执行时间(用于限流调度)")
    private LocalDateTime nextExecuteTime;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
