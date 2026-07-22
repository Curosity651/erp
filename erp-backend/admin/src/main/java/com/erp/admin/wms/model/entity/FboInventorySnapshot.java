package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wms_fbo_inventory_snapshot")
public class FboInventorySnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String platform;
    private Long shopId;
    private String platformWarehouseId;
    private String platformWarehouseName;
    private String platformItemId;
    private String skuCode;
    private Integer quantity;
    private String syncBatchNo;
    private LocalDateTime syncedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

