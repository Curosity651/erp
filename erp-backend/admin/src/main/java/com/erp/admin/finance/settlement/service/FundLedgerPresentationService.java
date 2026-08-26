package com.erp.admin.finance.settlement.service;

import com.erp.admin.finance.settlement.model.vo.FundLedgerMonthVO;
import com.erp.admin.finance.settlement.model.vo.FundLedgerVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FundLedgerPresentationService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final Map<String, String> FEE_LABELS = feeLabels();

    public List<FundLedgerMonthVO> group(List<FundLedgerVO> rows, Map<String, String> statementStatuses,
            LocalDate today) {
        return group(rows, statementStatuses, today, null, null);
    }

    public List<FundLedgerMonthVO> group(List<FundLedgerVO> rows, Map<String, String> statementStatuses,
            LocalDate today, LocalDate startDate, LocalDate endDate) {
        List<FundLedgerVO> ordered = new ArrayList<>(rows == null ? java.util.Collections.emptyList() : rows);
        ordered.sort(Comparator.comparing(FundLedgerVO::getOccurredTime,
                Comparator.nullsLast(Comparator.naturalOrder())));

        Map<String, FundLedgerMonthVO> months = new LinkedHashMap<>();
        BigDecimal running = money(BigDecimal.ZERO);
        for (FundLedgerVO row : ordered) {
            applyLabels(row);
            LocalDateTime occurred = row.getOccurredTime();
            if (occurred == null) continue;
            BigDecimal amount = money(row.getAmount());
            LocalDate date = occurred.toLocalDate();
            boolean selected = (startDate == null || !date.isBefore(startDate))
                    && (endDate == null || !date.isAfter(endDate));
            if (selected) {
                String monthKey = occurred.format(MONTH_FORMAT);
                FundLedgerMonthVO month = months.get(monthKey);
                if (month == null) {
                    month = createMonth(monthKey, row.getCurrency(), running, statementStatuses, today);
                    months.put(monthKey, month);
                }
                month.getDetails().add(row);
                month.setNetChange(money(month.getNetChange().add(amount)));
                if ("RECHARGE".equals(row.getEntryType()) && amount.signum() > 0) {
                    month.setRechargeAmount(money(month.getRechargeAmount().add(amount)));
                }
                else if ("REVERSAL".equals(row.getEntryType()) && amount.signum() < 0) {
                    month.setReversalAmount(money(month.getReversalAmount().add(amount.abs())));
                }
                else if ("CHARGE".equals(row.getEntryType()) && amount.signum() < 0) {
                    month.setChargeAmount(money(month.getChargeAmount().add(amount.abs())));
                }
            }
            running = money(running.add(amount));
            if (selected) {
                FundLedgerMonthVO month = months.get(occurred.format(MONTH_FORMAT));
                month.setClosingBalance(running);
            }
        }

        List<FundLedgerMonthVO> result = new ArrayList<>(months.values());
        result.forEach(month -> month.getDetails().sort(Comparator.comparing(FundLedgerVO::getOccurredTime).reversed()));
        result.sort(Comparator.comparing(FundLedgerMonthVO::getMonth).reversed());
        return result;
    }

    private FundLedgerMonthVO createMonth(String key, String currency, BigDecimal opening,
            Map<String, String> statuses, LocalDate today) {
        FundLedgerMonthVO month = new FundLedgerMonthVO();
        month.setMonth(key);
        month.setCurrency(currency);
        month.setOpeningBalance(money(opening));
        month.setClosingBalance(money(opening));
        month.setStatementStatus(YearMonth.from(today).toString().equals(key)
                ? "REALTIME" : statuses.getOrDefault(key, "UNBILLED"));
        return month;
    }

    private void applyLabels(FundLedgerVO row) {
        row.setMonth(row.getOccurredTime() == null ? null : row.getOccurredTime().format(MONTH_FORMAT));
        switch (value(row.getEntryType())) {
            case "RECHARGE": row.setEntryTypeLabel("充值入账"); break;
            case "REVERSAL": row.setEntryTypeLabel("充值冲正"); break;
            case "CHARGE": row.setEntryTypeLabel("服务费扣款"); break;
            default: row.setEntryTypeLabel("其他资金变动");
        }
        row.setBusinessLabel(businessLabel(row.getBusinessNo()));
        if ("RECHARGE".equals(row.getEntryType())) row.setDescriptionLabel("服务商充值");
        else if ("REVERSAL".equals(row.getEntryType())) row.setDescriptionLabel("充值冲正");
        else row.setDescriptionLabel(FEE_LABELS.getOrDefault(value(row.getDescription()),
                "其他服务费" + (row.getDescription() == null ? "" : "（" + row.getDescription() + "）")));
    }

    private String businessLabel(String businessNo) {
        if (businessNo == null) return "-";
        String[] parts = businessNo.split(":");
        if (parts.length >= 2) {
            if ("FULFILLMENT".equals(parts[0])) return "出库履约单 #" + parts[1];
            if ("INBOUND".equals(parts[0])) return "入库单 #" + parts[1];
            if ("OUTBOUND".equals(parts[0])) return "出库单 #" + parts[1];
        }
        return businessNo;
    }

    private static Map<String, String> feeLabels() {
        Map<String, String> labels = new java.util.HashMap<>();
        labels.put("INBOUND_CBM", "入库服务费（按体积）");
        labels.put("OUTBOUND_SMALL_ITEM", "小件出库服务费");
        labels.put("OUTBOUND_LARGE_ITEM", "大件出库服务费");
        labels.put("DELIVERY_TRUCK_ACTUAL", "整车配送费");
        labels.put("DELIVERY_LTL_SHARE", "零担配送分摊费");
        labels.put("DELIVERY_SHORT_SMALL_BOX", "短途配送费（小箱）");
        labels.put("DELIVERY_SHORT_LARGE_BOX", "短途配送费（大箱）");
        labels.put("RETURN_PICKUP_BOX", "退货取件费");
        labels.put("RETURN_INSPECTION_GENERAL", "普通商品退货质检费");
        labels.put("RETURN_INSPECTION_ELECTRONIC", "电子商品退货质检费");
        labels.put("BILL_ADJUSTMENT", "账单调整");
        return labels;
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
