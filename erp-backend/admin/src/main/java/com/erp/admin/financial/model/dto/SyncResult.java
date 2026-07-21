package com.erp.admin.financial.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步结果统计
 *
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "同步结果统计")
public class SyncResult {

    @Schema(description = "同步开始时间")
    private LocalDateTime startTime;

    @Schema(description = "同步结束时间")
    private LocalDateTime endTime;

    @Schema(description = "总店铺数")
    private int totalShops;

    @Schema(description = "成功店铺数")
    private int successShops;

    @Schema(description = "失败店铺数")
    private int failedShops;

    @Schema(description = "总记录数")
    private int totalRecords;

    @Schema(description = "各店铺详情")
    private List<ShopSyncResult> shopResults;
}
