package com.erp.admin.finance.settlement.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FundLedgerVO {
    private String month;
    private String entryType;
    private String entryTypeLabel;
    private String businessNo;
    private String businessLabel;
    private String description;
    private String descriptionLabel;
    private BigDecimal amount;
    private String currency;
    private String status;
    private Long voucherFileId;
    private LocalDateTime occurredTime;
    private String operatorName;
}
