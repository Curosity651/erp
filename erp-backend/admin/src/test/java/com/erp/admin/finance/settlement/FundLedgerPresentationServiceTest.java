package com.erp.admin.finance.settlement;

import com.erp.admin.finance.settlement.model.vo.FundLedgerMonthVO;
import com.erp.admin.finance.settlement.model.vo.FundLedgerVO;
import com.erp.admin.finance.settlement.service.FundLedgerPresentationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FundLedgerPresentationServiceTest {

    private final FundLedgerPresentationService service = new FundLedgerPresentationService();

    @Test
    void groupsSignedLedgerByMonthAndMarksCurrentMonthRealtime() {
        FundLedgerVO recharge = row("RECHARGE", "CZ20260801", "服务商充值",
                "50000.00", LocalDateTime.of(2026, 8, 1, 10, 0));
        FundLedgerVO charge = row("CHARGE", "FULFILLMENT:7:OUTBOUND_SMALL_ITEM",
                "OUTBOUND_SMALL_ITEM", "-186.69", LocalDateTime.of(2026, 8, 25, 20, 0));

        List<FundLedgerMonthVO> result = service.group(Arrays.asList(charge, recharge),
                Collections.emptyMap(), LocalDate.of(2026, 8, 26));

        assertEquals(1, result.size());
        FundLedgerMonthVO august = result.get(0);
        assertEquals("2026-08", august.getMonth());
        assertEquals(new BigDecimal("50000.00"), august.getRechargeAmount());
        assertEquals(new BigDecimal("186.69"), august.getChargeAmount());
        assertEquals(new BigDecimal("49813.31"), august.getNetChange());
        assertEquals(new BigDecimal("0.00"), august.getOpeningBalance());
        assertEquals(new BigDecimal("49813.31"), august.getClosingBalance());
        assertEquals("REALTIME", august.getStatementStatus());
    }

    @Test
    void exposesChineseLabelsWithoutDiscardingRawCodes() {
        FundLedgerVO charge = row("CHARGE", "INBOUND:31:INBOUND_CBM", "INBOUND_CBM",
                "-69.79", LocalDateTime.of(2026, 8, 22, 15, 16));

        service.group(Collections.singletonList(charge), Collections.emptyMap(),
                LocalDate.of(2026, 8, 26));

        assertEquals("服务费扣款", charge.getEntryTypeLabel());
        assertEquals("入库单 #31", charge.getBusinessLabel());
        assertEquals("入库服务费（按体积）", charge.getDescriptionLabel());
        assertEquals("INBOUND_CBM", charge.getDescription());
    }

    @Test
    void labelsLogisticsProductLedgerUsingItsPersistedBusinessFormat() {
        FundLedgerVO charge = row("CHARGE", "FULFILLMENT_LOGISTICS:7", "小件经济自提",
                "-10.00", LocalDateTime.of(2026, 8, 25, 20, 0));

        service.group(Collections.singletonList(charge), Collections.emptyMap(),
                LocalDate.of(2026, 8, 26));

        assertEquals("履约订单 #7", charge.getBusinessLabel());
        assertEquals("物流产品费用（小件经济自提）", charge.getDescriptionLabel());
    }

    private static FundLedgerVO row(String type, String businessNo, String description,
            String amount, LocalDateTime time) {
        FundLedgerVO row = new FundLedgerVO();
        row.setEntryType(type);
        row.setBusinessNo(businessNo);
        row.setDescription(description);
        row.setAmount(new BigDecimal(amount));
        row.setCurrency("CNY");
        row.setOccurredTime(time);
        return row;
    }
}
