package com.erp.admin.platform.finance.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BillingRecordVO {
    private Long id;
    private Long erpTenantId;
    private String ownerName;
    private Long warehouseId;
    private String feeType;
    private String feeCode;
    private String feeName;
    private String billingUnit;
    private BigDecimal billingQuantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String currency;
    private String sourceType;
    private String sourceRef;
    private String remark;

    private Long operatorId;

    private String operatorName;
    private LocalDateTime createTime;
}
