package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wms_location_slot")
public class WmsLocationSlot {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long warehouseId;
    private Long locationId;
    private Integer levelNo;
    private Integer positionNo;
    private String slotCode;
    private Integer maxHeightMm;
    private BigDecimal maxWeightKg;
    private String slotStatus;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
