package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FBO库存同步日志实体
 *
 * @author erp
 */
@Data
@TableName(value = "wms_fbo_sync_log", autoResultMap = true)
@Schema(title = "FBO同步日志实体")
public class FboSyncLog {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    private Long tenantId;

    @Schema(title = "日志编号")
    private String logNo;

    @Schema(title = "平台")
    private String platform;

    @Schema(title = "店铺ID")
    private Long shopId;

    @Schema(title = "同步类型: SCHEDULED/MANUAL")
    private String syncType;

    private String syncBatchNo;

    @Schema(title = "总数量")
    private Integer totalCount;

    private Integer sourceCount;

    @Schema(title = "成功数量")
    private Integer successCount;

    @Schema(title = "失败数量")
    private Integer failCount;

    @Schema(title = "未映射数量")
    private Integer unmappedCount;

    private Integer snapshotQuantity;

    @Schema(title = "同步状态: SUCCESS/PARTIAL/FAILED")
    private String syncStatus;

    @Schema(title = "错误信息")
    private String errorMessage;

    @Schema(title = "失败明细JSON")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<FboSyncFailDetail> failDetails;

    @Schema(title = "自动创建仓库数量")
    private Integer autoCreatedWarehouseCount;

    @Schema(title = "自动创建的仓库列表JSON")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<AutoCreatedWarehouse> autoCreatedWarehouses;

    @Schema(title = "同步时间")
    private LocalDateTime syncTime;

    @Schema(title = "耗时（毫秒）")
    private Integer duration;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 失败明细内部类
     */
    @Data
    public static class FboSyncFailDetail {
        private String platformItemId;
        private String failReason;
        private String message;

        public FboSyncFailDetail() {}

        public FboSyncFailDetail(String platformItemId, String failReason, String message) {
            this.platformItemId = platformItemId;
            this.failReason = failReason;
            this.message = message;
        }
    }

    /**
     * 自动创建仓库内部类
     */
    @Data
    public static class AutoCreatedWarehouse {
        private String warehouseCode;
        private String warehouseName;
        private String platformWarehouseId;

        public AutoCreatedWarehouse() {}

        public AutoCreatedWarehouse(String warehouseCode, String warehouseName, String platformWarehouseId) {
            this.warehouseCode = warehouseCode;
            this.warehouseName = warehouseName;
            this.platformWarehouseId = platformWarehouseId;
        }
    }

}
