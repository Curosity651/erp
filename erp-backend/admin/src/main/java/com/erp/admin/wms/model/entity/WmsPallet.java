package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wms_pallet")
public class WmsPallet {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String palletNo;
    private Long warehouseId;
    private Long slotId;
    private Long currentSlotId;
    private String slotCode;
    private String palletType;
    private String palletStatus;
    private BigDecimal capacityPercent;
    private String capacitySource;
    private BigDecimal estimatedVolumeCbm;
    private BigDecimal estimatedWeightKg;
    private BigDecimal actualWeightKg;
    private Integer skuKindCount;
    private Integer wholePalletEligible;
    private String lockedReason;
    @Version
    private Integer version;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}

