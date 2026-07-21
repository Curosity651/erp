package com.erp.admin.financial.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WB 财务同步作业表
 * <p>
 * 一次同步请求创建一个 Job，包含多个店铺任务
 *
 * @author system
 */
@TableName("wb_financial_sync_job")
@Data
@Schema(description = "WB财务同步作业")
public class WbFinancialSyncJob {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "作业编号(唯一)")
    private String jobCode;

    @Schema(description = "同步类型: FULL-全量/INCREMENTAL-增量")
    private String syncType;

    @Schema(description = "报表周期类型：weekly-周报, daily-日报")
    private String periodType;

    @Schema(description = "开始日期")
    private LocalDate dateFrom;

    @Schema(description = "结束日期")
    private LocalDate dateTo;

    @Schema(description = "总店铺数")
    private Integer totalShops;

    @Schema(description = "已完成店铺数")
    private Integer completedShops;

    @Schema(description = "失败店铺数")
    private Integer failedShops;

    @Schema(description = "总记录数")
    private Integer totalRecords;

    @Schema(description = "状态: RUNNING-运行中/COMPLETED-已完成/FAILED-失败")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
