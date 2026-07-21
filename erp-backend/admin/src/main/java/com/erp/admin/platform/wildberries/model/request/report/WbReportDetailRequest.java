package com.erp.admin.platform.wildberries.model.request.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WB 财务报表查询请求
 * <p>
 * API: GET /api/v5/supplier/reportDetailByPeriod
 *
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "WB 财务报表查询请求")
public class WbReportDetailRequest {

    @Schema(description = "报表开始日期（RFC3339 格式，如：2024-01-01 或 2024-01-01T00:00:00）", required = true)
    private String dateFrom;

    @Schema(description = "报表结束日期（RFC3339 格式，如：2024-01-31）", required = true)
    private String dateTo;

    @Schema(description = "每页返回的记录数（最大 100000）", example = "100000")
    private Integer limit;

    @Schema(description = "分页游标（rrd_id，用于获取下一页数据，首次请求传 0）", example = "0")
    private Long rrdid;

    @Schema(description = "报表周期（weekly=周报，daily=日报）", example = "weekly")
    private String period;
}
