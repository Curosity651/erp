package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存配置实体
 *
 * @author erp
 */
@Data
@TableName("wms_inventory_config")
@Schema(title = "库存配置实体")
public class InventoryConfig {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "归属货主 erp_tenant_id（数据级隔离，新建时自动盖章）")
    private Long erpTenantId;

    @Schema(title = "区域ID")
    private Long regionId;

    @Schema(title = "SKU编码")
    private String skuCode;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(title = "安全库存数量，NULL表示使用全局配置")
    private Integer safetyStock;

    @Schema(title = "是否启用通知")
    private Boolean notifyEnabled;

	@TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(title = "预警阈值天数，NULL表示使用全局配置")
    private Integer notifyThresholdDays;

    @Schema(title = "创建人")
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;
}
