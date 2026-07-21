package com.erp.admin.order.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("erp_label_batch")
@Schema(title = "面单打印批次")
public class ErpLabelBatch {

    @TableId
    private Long id;

    @Schema(title = "批次号")
    private String batchNo;

    @Schema(title = "平台: wildberries|ozon")
    private String platform;

    @Schema(title = "订单总数")
    private Integer totalOrders;

    @Schema(title = "文件总数")
    private Integer totalFiles;

    @Schema(title = "状态: CREATED|GENERATED|UPLOADED|FAILED")
    private String status;

    @Schema(title = "成功订单数")
    private Integer successCount;

    @Schema(title = "失败订单数")
    private Integer failedCount;

    @Schema(title = "成功文件数")
    private Integer fileSuccess;

    @Schema(title = "失败文件数")
    private Integer fileFailed;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建人")
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}


