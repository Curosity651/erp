package com.erp.admin.financial.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步任务响应
 *
 * @author system
 */
@Data
@Builder
@Schema(description = "同步任务响应")
public class SyncTaskResponse {

    @Schema(description = "作业ID")
    private Long jobId;

    @Schema(description = "作业编号")
    private String jobCode;

    @Schema(description = "同步类型")
    private String syncType;

    @Schema(description = "待同步店铺数")
    private Integer totalShops;

    @Schema(description = "任务ID列表")
    private List<Long> taskIds;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "消息")
    private String message;
}
