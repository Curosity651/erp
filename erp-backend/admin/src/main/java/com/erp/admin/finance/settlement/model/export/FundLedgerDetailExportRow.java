package com.erp.admin.finance.settlement.model.export;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ColumnWidth(20)
public class FundLedgerDetailExportRow {
    @ExcelProperty("发生时间") private LocalDateTime occurredTime;
    @ExcelProperty("月份") private String month;
    @ExcelProperty("业务类型") private String entryType;
    @ExcelProperty("业务单据") private String businessLabel;
    @ExcelProperty("费用项目") private String descriptionLabel;
    @ExcelProperty("收入金额") private BigDecimal incomeAmount;
    @ExcelProperty("支出金额") private BigDecimal expenseAmount;
    @ExcelProperty("币种") private String currency;
    @ExcelProperty("操作人") private String operatorName;
    @ExcelProperty("原始业务号") private String businessNo;
}
