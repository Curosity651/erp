package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * FBO同步结果VO
 *
 * @author erp
 */
@Data
@Builder
@Schema(title = "FBO同步结果VO")
public class FboSyncResultVO {

    @Schema(title = "日志编号")
    private String logNo;

    @Schema(title = "总数量")
    private Integer totalCount;

    @Schema(title = "成功数量")
    private Integer successCount;

    @Schema(title = "失败数量")
    private Integer failCount;

    @Schema(title = "未映射数量")
    private Integer unmappedCount;

    @Schema(title = "同步状态")
    private String syncStatus;

    @Schema(title = "自动创建仓库数量")
    private Integer autoCreatedWarehouseCount;

}
