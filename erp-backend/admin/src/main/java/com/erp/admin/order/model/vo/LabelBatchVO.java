package com.erp.admin.order.model.vo;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "面单批次详情")
public class LabelBatchVO {

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

    @Schema(title = "批次状态")
    private String status;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建人ID")
    private Long createdBy;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @Schema(title = "成功订单数")
    private Integer successCount;

    @Schema(title = "失败订单数")
    private Integer failedCount;

    @Schema(title = "文件列表")
    private List<LabelBatchFileVO> files;

    @Schema(title = "失败的订单项列表")
    private List<LabelBatchItemVO> failedItems;
}


