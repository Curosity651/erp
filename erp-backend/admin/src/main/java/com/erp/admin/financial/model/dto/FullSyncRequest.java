package com.erp.admin.financial.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 全量同步请求
 *
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "全量同步请求")
public class FullSyncRequest {

    @Schema(description = "开始日期", example = "2024-01-01")
    private String dateFrom;

    @Schema(description = "结束日期", example = "2024-12-31")
    private String dateTo;

    @Schema(description = "店铺ID列表，可选。为空时同步全部启用的 Wildberries 店铺", example = "[1001,1002]")
    private List<Long> shopIds;

    @Schema(description = "报表周期类型：weekly-周报, daily-日报，默认 weekly", example = "weekly", allowableValues = {"weekly", "daily"})
    @Builder.Default
    private String periodType = "weekly";
}
