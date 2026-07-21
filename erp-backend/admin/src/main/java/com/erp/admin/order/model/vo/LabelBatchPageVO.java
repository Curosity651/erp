package com.erp.admin.order.model.vo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 面单批次分页列表 VO
 * 用于分页查询，不包含文件列表和失败订单项详情
 */
@Data
@Schema(title = "面单批次分页列表")
public class LabelBatchPageVO {

    @Schema(title = "批次ID")
    private Long batchId;

    @Schema(title = "批次号")
    private String batchNo;

    @Schema(title = "平台")
    private String platform;

    @Schema(title = "订单总数")
    private Integer totalOrders;

    @Schema(title = "文件总数")
    private Integer totalFiles;

    @Schema(title = "成功订单数")
    private Integer successCount;

    @Schema(title = "失败订单数")
    private Integer failedCount;

    @Schema(title = "批次状态")
    private String status;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建人ID")
    private Long createdBy;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;
}
