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
 * 月末生成当月应收账单，并在次月初幂等重算一次以纳入月末最后一分钟的业务流水。
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

    @Scheduled(cron = "0 59 23 L * ?")
    public void generateCurrentMonthBills() {
        generateMonth(YearMonth.now().format(MONTH), "月末");
    }

    @Scheduled(cron = "0 5 0 1 * ?")
    public void reconcileLastMonthBills() {
        generateMonth(YearMonth.now().minusMonths(1).format(MONTH), "月初复核");
    }

    private void generateMonth(String month, String scene) {
        try {
            GenerateBillResultVO r = monthlyBillService.generateForMonth(month, null);
            log.info("{}账单自动生成完成, month={}, created={}, recalculated={}, skipped={}", scene, month,
                    r.getCreated(), r.getRecalculated(), r.getSkipped());
        } catch (Exception e) {
            log.error("{}账单自动生成失败, month={}", scene, month, e);
        }
    }

}
