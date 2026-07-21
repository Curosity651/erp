package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wms_pallet_operation_log")
public class WmsPalletOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long palletId;
    private String operationType;
    private String fromSlotCode;
    private String toSlotCode;
    private BigDecimal capacityPercent;
    private String remark;
    private Long operatorId;
    private LocalDateTime createTime;
}

