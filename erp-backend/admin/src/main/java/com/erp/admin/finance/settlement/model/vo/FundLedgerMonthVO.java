package com.erp.admin.finance.settlement.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class FundLedgerMonthVO {
    private String month;
    private BigDecimal rechargeAmount = BigDecimal.ZERO.setScale(2);
    private BigDecimal chargeAmount = BigDecimal.ZERO.setScale(2);
    private BigDecimal reversalAmount = BigDecimal.ZERO.setScale(2);
    private BigDecimal netChange = BigDecimal.ZERO.setScale(2);
    private BigDecimal openingBalance = BigDecimal.ZERO.setScale(2);
    private BigDecimal closingBalance = BigDecimal.ZERO.setScale(2);
    private String currency;
    private String statementStatus;
    private List<FundLedgerVO> details = new ArrayList<>();
}
