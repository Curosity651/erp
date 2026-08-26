package com.erp.admin.finance.settlement;

import com.erp.admin.finance.settlement.model.vo.FundLedgerMonthVO;
import com.erp.admin.finance.settlement.model.vo.FundLedgerVO;
import com.erp.admin.finance.settlement.service.FundLedgerExportService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FundLedgerExportServiceTest {
    @Test
    void writesMonthlySummaryAndDetailedLedgerSheets() {
        FundLedgerVO detail = new FundLedgerVO();
        detail.setMonth("2026-08");
        detail.setEntryTypeLabel("服务费扣款");
        detail.setBusinessLabel("入库单 #31");
        detail.setDescriptionLabel("入库服务费（按体积）");
        detail.setAmount(new BigDecimal("-69.79"));
        detail.setCurrency("CNY");
        detail.setOccurredTime(LocalDateTime.of(2026, 8, 22, 15, 16));

        FundLedgerMonthVO month = new FundLedgerMonthVO();
        month.setMonth("2026-08");
        month.setCurrency("CNY");
        month.setChargeAmount(new BigDecimal("69.79"));
        month.setNetChange(new BigDecimal("-69.79"));
        month.setStatementStatus("REALTIME");
        month.setDetails(Collections.singletonList(detail));

        byte[] bytes = new FundLedgerExportService().export(Collections.singletonList(month));
        assertTrue(bytes.length > 1000);
        assertTrue(bytes[0] == 'P' && bytes[1] == 'K');
    }
}
