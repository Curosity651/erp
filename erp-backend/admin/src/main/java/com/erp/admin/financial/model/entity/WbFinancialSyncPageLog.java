package com.erp.admin.financial.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WB 财务同步分页执行日志表
 * <p>
 * 记录每次 API 调用的详细信息
 *
 * @author system
 */
@TableName("wb_financial_sync_page_log")
@Data
@Schema(description = "WB财务同步分页执行日志")
public class WbFinancialSyncPageLog {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "所属任务ID")
    private Long taskId;

    @Schema(description = "页码")
    private Integer pageNum;

    @Schema(description = "本次请求的rrd_id")
    private Long rrdId;

    @Schema(description = "本次获取记录数")
    private Integer recordCount;

    @Schema(description = "状态: SUCCESS-成功/FAILED-失败")
    private String status;

    @Schema(description = "执行时间")
    private LocalDateTime executeTime;

    @Schema(description = "耗时(毫秒)")
    private Integer durationMs;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
