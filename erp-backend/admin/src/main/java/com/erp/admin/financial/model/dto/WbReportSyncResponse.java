package com.erp.admin.financial.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WB 财务报表同步响应
 *
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "WB 财务报表同步响应")
public class WbReportSyncResponse {

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "消息")
    private String message;

    @Schema(description = "同步结果详情")
    private SyncResult data;

    /**
     * 创建成功响应
     *
     * @param syncResult 同步结果
     * @return 响应对象
     */
    public static WbReportSyncResponse success(SyncResult syncResult) {
        return WbReportSyncResponse.builder()
                .success(true)
                .message("同步完成")
                .data(syncResult)
                .build();
    }

    /**
     * 创建失败响应
     *
     * @param message 错误消息
     * @return 响应对象
     */
    public static WbReportSyncResponse error(String message) {
        return WbReportSyncResponse.builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
