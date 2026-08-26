package com.erp.admin.finance.settlement.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FundAccountVO {
    private String accountScope;
    private Long wmsTenantId;
    private String wmsTenantName;
    private Long erpTenantId;
    private String erpTenantName;
    private String currency;
    private BigDecimal rechargeAmount;
    private BigDecimal chargeAmount;
    private BigDecimal balance;
    private Boolean negative;
    private Integer pendingCount;
    private LocalDateTime lastChangeTime;
}
