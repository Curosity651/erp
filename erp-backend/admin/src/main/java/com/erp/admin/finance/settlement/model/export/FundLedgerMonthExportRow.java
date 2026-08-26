package com.erp.admin.finance.settlement.model.export;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ColumnWidth(16)
public class FundLedgerMonthExportRow {
    @ExcelProperty("月份") private String month;
    @ExcelProperty("充值合计") private BigDecimal rechargeAmount;
    @ExcelProperty("服务费合计") private BigDecimal chargeAmount;
    @ExcelProperty("冲正金额") private BigDecimal reversalAmount;
    @ExcelProperty("净变动") private BigDecimal netChange;
    @ExcelProperty("期初余额") private BigDecimal openingBalance;
    @ExcelProperty("期末余额") private BigDecimal closingBalance;
    @ExcelProperty("对账状态") private String statementStatus;
    @ExcelProperty("币种") private String currency;
}
