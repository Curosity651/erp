package com.erp.admin.financial.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务进度响应
 *
 * @author system
 */
@Data
@Builder
@Schema(description = "任务进度响应")
public class TaskProgressResponse {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "作业ID")
    private Long jobId;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "已完成页数")
    private Integer completedPages;

    @Schema(description = "总页数(预估)")
    private Integer totalPages;

    @Schema(description = "已同步记录数")
    private Integer totalRecords;

    @Schema(description = "进度百分比(0-100)")
    private Integer progressPercent;

    @Schema(description = "下次执行时间")
    private LocalDateTime nextExecuteTime;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
