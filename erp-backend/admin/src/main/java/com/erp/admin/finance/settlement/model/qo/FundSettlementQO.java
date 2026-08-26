package com.erp.admin.finance.settlement.model.qo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class FundSettlementQO {
    private Long erpTenantId;
    private Long wmsTenantId;
    private String currency;
    private String status;
    private Boolean negative;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
