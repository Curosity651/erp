package com.erp.admin.platform.finance.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("wms_fee_rate_card")
public class WmsFeeRateCard {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long wmsTenantId;
    private String feeCode;
    private String feeName;
    private String feeType;
    private String billingUnit;
    private BigDecimal unitPrice;
    private String currency;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

