package com.erp.admin.finance.settlement;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FundSettlementSqlContractTest {
    @Test
    void migrationCreatesRechargeLedgerAndBothMenus() throws Exception {
        String sql = read(Paths.get("..", "sql", "migration", "V132__fund_settlement_recharge.sql"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS wms_recharge_order"));
        assertTrue(sql.contains("uk_recharge_no"));
        assertTrue(sql.contains("170603"));
        assertTrue(sql.contains("资金结算"));
    }

    @Test
    void balanceUsesSourceLedgersAndDoesNotDeductMonthlyBillAgain() throws Exception {
        String xml = read(Paths.get("src", "main", "resources", "mapper", "financial",
                "FundSettlementQueryMapper.xml"));
        assertTrue(xml.contains("wms_client_billing_record"));
        assertTrue(xml.contains("fee_type = 'SHIPPING'"));
        assertTrue(xml.contains("wms_billing_record"));
        assertTrue(xml.contains("status = 'APPROVED'"));
        assertFalse(xml.contains("SUM(total_amount)"));
        assertTrue(xml.contains("MAX(create_time) last_time"));
        assertTrue(xml.contains("COALESCE(p.last_time"));
        assertFalse(xml.contains("'1970-01-01'"));
    }

    @Test
    void reversalLedgerKeepsOriginalRechargeAndAddsNegativeAuditEntry() throws Exception {
        String xml = read(Paths.get("src", "main", "resources", "mapper", "financial",
                "FundSettlementQueryMapper.xml"));
        assertTrue(xml.contains("recharge_no LIKE 'CZC%'"));
        assertTrue(xml.contains("WHEN recharge_no LIKE 'CZC%' THEN -amount"));
        assertTrue(xml.contains("status IN ('APPROVED','REVERSED')"));
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
