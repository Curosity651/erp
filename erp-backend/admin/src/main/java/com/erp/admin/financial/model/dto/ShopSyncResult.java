package com.erp.admin.financial.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单店铺同步结果
 *
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "单店铺同步结果")
public class ShopSyncResult {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "拉取的记录数")
    private int recordCount;

    @Schema(description = "耗时（毫秒）")
    private long durationMs;

    @Schema(description = "错误信息（如果失败）")
    private String errorMessage;
}
