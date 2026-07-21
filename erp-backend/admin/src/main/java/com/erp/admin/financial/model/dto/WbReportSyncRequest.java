package com.erp.admin.financial.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * WB 财务报表同步请求
 *
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "WB 财务报表同步请求")
public class WbReportSyncRequest {

    @NotBlank(message = "开始日期不能为空")
    @Schema(description = "开始日期（格式：yyyy-MM-dd）", example = "2024-01-01")
    private String dateFrom;

    @NotBlank(message = "结束日期不能为空")
    @Schema(description = "结束日期（格式：yyyy-MM-dd）", example = "2024-01-31")
    private String dateTo;
}
