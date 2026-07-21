package com.erp.admin.sync.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("sync_cursor")
@Schema(title = "同步游标")
public class SyncCursor {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(title = "店铺 ID")
    private Long shopId;

    @Schema(title = "平台: Wildberries / Ozon / Yandex")
    private String platform;

    @Schema(title = "任务类型: ORDER_INCREMENTAL / ORDER_STATUS")
    private String taskType;

    @Schema(title = "游标时间（UTC）")
    private LocalDateTime cursorTime;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;
}
