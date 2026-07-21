package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wms_region_inventory")
@Schema(title = "区域库存实体")
public class RegionInventory {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "货主维（货物主人），区域库存按货主隔离")
    private Long erpTenantId;

    @Schema(title = "区域ID")
    private Long regionId;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "区域级预占数量")
    private Integer reservedQuantity;

    @Schema(title = "区域级在途数量")
    private Integer inTransitQuantity;

    @Version
    @Schema(title = "乐观锁版本号")
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;
}
