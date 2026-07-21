package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * FBO同步日志分页VO
 *
 * @author erp
 */
@Data
@Schema(title = "FBO同步日志分页VO")
public class FboSyncLogPageVO {

    @Schema(title = "日志ID")
    private Long id;

    @Schema(title = "日志编号")
    private String logNo;

    @Schema(title = "平台")
    private String platform;

    @Schema(title = "店铺ID")
    private Long shopId;

    @Schema(title = "店铺名称")
    private String shopName;

    @Schema(title = "同步类型")
    private String syncType;

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

    @Schema(title = "错误信息")
    private String errorMessage;

    @Schema(title = "同步时间")
    private LocalDateTime syncTime;

    @Schema(title = "耗时（毫秒）")
    private Integer duration;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;

}
