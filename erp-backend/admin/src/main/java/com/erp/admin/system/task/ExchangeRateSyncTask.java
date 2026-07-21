package com.erp.admin.system.task;

import com.erp.admin.system.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 汇率同步定时任务
 *
 * @author system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateSyncTask {

	private final ExchangeRateService exchangeRateService;

	/**
	 * 每天早上6点执行汇率同步任务
	 * 同步昨日的EOD汇率,作为今日订单使用的汇率
	 * cron表达式: 秒 分 时 日 月 周
	 */
	@Scheduled(cron = "0 0 6 * * ?")
	public void syncExchangeRates() {
		log.info("开始执行汇率同步定时任务");
		try {
			int count = exchangeRateService.syncLatestRatesFromFixer();
			log.info("汇率同步任务执行完成,成功同步{}条记录", count);
		}
		catch (Exception e) {
			log.error("汇率同步任务执行失败", e);
		}
	}

	/**
	 * 手动触发汇率同步(用于测试)
	 * 可以通过调用此方法来手动触发同步
	 */
	public void manualSync() {
		log.info("手动触发汇率同步");
		syncExchangeRates();
	}

}
