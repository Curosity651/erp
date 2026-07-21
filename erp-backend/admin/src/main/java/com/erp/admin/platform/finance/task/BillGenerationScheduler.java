package com.erp.admin.platform.finance.task;

import com.erp.admin.platform.finance.model.vo.GenerateBillResultVO;
import com.erp.admin.platform.finance.service.MonthlyBillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 月初自动生成上月应收账单（业务需求 1.7 决策 K）。每月 1 日 02:00 汇总上月，全部 WMS 服务商。
 * 复用 {@link MonthlyBillService#generateForMonth}（无平台身份断言，供后台任务调用）：草稿覆盖，已确认/已付款跳过。
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillGenerationScheduler {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MonthlyBillService monthlyBillService;

    /** 每月 1 日 02:00 生成上月账单。 */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void generateLastMonthBills() {
        String lastMonth = YearMonth.now().minusMonths(1).format(MONTH);
        try {
            GenerateBillResultVO r = monthlyBillService.generateForMonth(lastMonth, null);
            log.info("月度账单自动生成完成, month={}, created={}, recalculated={}, skipped={}", lastMonth,
                    r.getCreated(), r.getRecalculated(), r.getSkipped());
        } catch (Exception e) {
            log.error("月度账单自动生成失败, month={}", lastMonth, e);
        }
    }

}
