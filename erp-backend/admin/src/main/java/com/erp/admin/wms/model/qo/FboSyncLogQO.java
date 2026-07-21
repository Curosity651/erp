package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * FBO同步日志查询条件
 *
 * @author erp
 */
@Data
@Schema(title = "FBO同步日志查询条件")
public class FboSyncLogQO {

    @Schema(title = "平台")
    private String platform;

    @Schema(title = "店铺ID")
    private Long shopId;

    @Schema(title = "同步类型")
    private String syncType;

    @Schema(title = "同步状态")
    private String syncStatus;

    @Schema(title = "同步时间开始")
    private LocalDateTime syncTimeStart;

    @Schema(title = "同步时间结束")
    private LocalDateTime syncTimeEnd;

}
