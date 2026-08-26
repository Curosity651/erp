package com.erp.admin.finance.settlement.service;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.write.metadata.WriteSheet;
import com.erp.admin.finance.settlement.model.export.FundLedgerDetailExportRow;
import com.erp.admin.finance.settlement.model.export.FundLedgerMonthExportRow;
import com.erp.admin.finance.settlement.model.vo.FundLedgerMonthVO;
import com.erp.admin.finance.settlement.model.vo.FundLedgerVO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class FundLedgerExportService {

    public byte[] export(List<FundLedgerMonthVO> months) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ExcelWriter writer = FastExcel.write(output).build();
            try {
                WriteSheet summarySheet = FastExcel.writerSheet(0, "月度汇总")
                        .head(FundLedgerMonthExportRow.class).build();
                writer.write(monthRows(months), summarySheet);
                WriteSheet detailSheet = FastExcel.writerSheet(1, "流水明细")
                        .head(FundLedgerDetailExportRow.class).build();
                writer.write(detailRows(months), detailSheet);
            }
            finally {
                writer.finish();
            }
            return output.toByteArray();
        }
        catch (Exception ex) {
            throw new IllegalStateException("导出资金流水失败", ex);
        }
    }

    private List<FundLedgerMonthExportRow> monthRows(List<FundLedgerMonthVO> months) {
        List<FundLedgerMonthExportRow> rows = new ArrayList<>();
        for (FundLedgerMonthVO source : months) {
            FundLedgerMonthExportRow row = new FundLedgerMonthExportRow();
            row.setMonth(source.getMonth());
            row.setRechargeAmount(source.getRechargeAmount());
            row.setChargeAmount(source.getChargeAmount());
            row.setReversalAmount(source.getReversalAmount());
            row.setNetChange(source.getNetChange());
            row.setOpeningBalance(source.getOpeningBalance());
            row.setClosingBalance(source.getClosingBalance());
            row.setStatementStatus(statusLabel(source.getStatementStatus()));
            row.setCurrency(source.getCurrency());
            rows.add(row);
        }
        return rows;
    }

    private List<FundLedgerDetailExportRow> detailRows(List<FundLedgerMonthVO> months) {
        List<FundLedgerDetailExportRow> rows = new ArrayList<>();
        for (FundLedgerMonthVO month : months) {
            for (FundLedgerVO source : month.getDetails()) {
                FundLedgerDetailExportRow row = new FundLedgerDetailExportRow();
                row.setOccurredTime(source.getOccurredTime());
                row.setMonth(source.getMonth());
                row.setEntryType(source.getEntryTypeLabel());
                row.setBusinessLabel(source.getBusinessLabel());
                row.setDescriptionLabel(source.getDescriptionLabel());
                BigDecimal amount = source.getAmount() == null ? BigDecimal.ZERO : source.getAmount();
                row.setIncomeAmount(amount.signum() > 0 ? amount : BigDecimal.ZERO);
                row.setExpenseAmount(amount.signum() < 0 ? amount.abs() : BigDecimal.ZERO);
                row.setCurrency(source.getCurrency());
                row.setOperatorName(source.getOperatorName());
                row.setBusinessNo(source.getBusinessNo());
                rows.add(row);
            }
        }
        return rows;
    }

    private String statusLabel(String status) {
        if ("REALTIME".equals(status)) return "实时统计";
        if ("DRAFT".equals(status)) return "待复核";
        if ("CONFIRMED".equals(status)) return "已复核";
        if ("PAID".equals(status)) return "已付款";
        return "未生成对账单";
    }
}
