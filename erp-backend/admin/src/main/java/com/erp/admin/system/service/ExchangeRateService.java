package com.erp.admin.system.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.erp.admin.system.config.FixerApiProperties;
import com.erp.admin.system.mapper.ExchangeRateMapper;
import com.erp.admin.system.model.dto.FixerApiResponse;
import com.erp.admin.system.model.entity.ExchangeRate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * 汇率服务
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService extends ExtendServiceImpl<ExchangeRateMapper, ExchangeRate> {

	private final RestTemplate restTemplate;

	private final FixerApiProperties fixerApiProperties;

	/**
	 * 当前使用的API Key索引
	 */
	private final AtomicInteger currentKeyIndex = new AtomicInteger(0);

	private static final String SOURCE_FIXER = "Fixer.io";

	/**
	 * 目标货币
	 */
	private static final String TARGET_CURRENCY = "CNY";

	/**
	 * Fixer.io 免费计划固定的基准货币
	 */
	private static final String FIXER_BASE_CURRENCY = "EUR";

	/**
	 * 需要同步汇率的基准货币列表(相对于CNY)
	 */
	private static final List<String> BASE_CURRENCIES = Arrays.asList("RUB", "BYN", "KZT", "KGS", "AMD", "USD", "UZS", "THB");

	/**
	 * 需要从Fixer.io同步的所有货币列表(BASE_CURRENCIES + TARGET_CURRENCY)
	 */
	private static final List<String> SYNC_CURRENCIES;

	static {
		List<String> currencies = new ArrayList<>(BASE_CURRENCIES);
		currencies.add(TARGET_CURRENCY);
		SYNC_CURRENCIES = Collections.unmodifiableList(currencies);
	}


	/**
	 * 调用Fixer API(支持多Key轮询)
	 * 当一个Key失败时,自动尝试下一个Key
	 * 
	 * @param date 汇率日期
	 * @return API响应结果
	 */
	private FixerApiResponse callFixerApiWithRetry(LocalDate date) {
		List<String> apiKeys = fixerApiProperties.getKeys();
		if (apiKeys.isEmpty()) {
			log.error("Fixer.io API Keys未配置,请在application.yml中配置fixer.api.keys");
			return null;
		}

		String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
		String symbols = String.join(",", SYNC_CURRENCIES);
		int startIndex = currentKeyIndex.get();

		// 尝试所有的API Key
		for (int i = 0; i < apiKeys.size(); i++) {
			int keyIndex = (startIndex + i) % apiKeys.size();
			String apiKey = apiKeys.get(keyIndex).trim();

			try {
				String url = String.format("%s/%s?access_key=%s&base=%s&symbols=%s",
						fixerApiProperties.getBaseUrl(), dateStr, apiKey, FIXER_BASE_CURRENCY, symbols);
				log.info("调用 Fixer Historical API (Key索引:{}): {}", keyIndex, url);

				FixerApiResponse response = restTemplate.getForObject(url, FixerApiResponse.class);

				if (response != null && Boolean.TRUE.equals(response.getSuccess())) {
					// 成功后更新当前使用的Key索引
					currentKeyIndex.set(keyIndex);
					return response;
				}

				// API调用失败,检查是否是配额不足
				if (response != null && response.getError() != null) {
					FixerApiResponse.Error error = response.getError();

					// 429: 配额超限 (Too Many Requests), 尝试下一个Key
					// 104: Monthly request quota reached (某些情况下也表示配额不足)
					if (error.getCode() != null && (error.getCode() == 429 || error.getCode() == 104)) {
						log.warn("API Key索引:{} 配额已用尽,尝试下一个Key. 错误码: {}, 错误信息: {}",
								keyIndex, error.getCode(), error.getInfo());
						continue;
					}

					log.error("Fixer.io API调用失败 (Key索引:{}): code={}, info={}",
							keyIndex, error.getCode(), error.getInfo());
				}
			} catch (HttpClientErrorException e) {
				// 捕获 HTTP 429 状态码异常
				if (e.getRawStatusCode() == 429) {
					log.warn("API Key索引:{} 配额已用尽(HTTP 429),尝试下一个Key", keyIndex);
					continue;
				}
				log.error("调用Fixer API失败 (Key索引:{}, HTTP状态码:{})", keyIndex, e.getRawStatusCode(), e);
			} catch (Exception e) {
				log.error("调用Fixer API失败 (Key索引:{})", keyIndex, e);
			}
		}

		log.error("所有API Key均调用失败");
		return null;
	}

	/**
	 * 批量获取或同步汇率
	 * 
	 * @param currencyCodes 需要转换的币种列表
	 * @param targetCurrency 目标币种 (通常是CNY)
	 * @param rateDate 汇率日期
	 * @return Map<币种, 汇率> 例如: {"RUB": 0.078, "BYN": 2.15}
	 */
	@Transactional(rollbackFor = Exception.class)
	public Map<String, BigDecimal> batchGetOrSyncRates(
			Collection<String> currencyCodes,
			String targetCurrency,
			LocalDate rateDate) {
		
		Map<String, BigDecimal> result = new HashMap<>();
		List<String> missingCurrencies = new ArrayList<>();

		// 1. 先从数据库批量查询
		for (String currency : currencyCodes) {
			// 如果源币种和目标币种相同，直接返回1.0
			if (currency.equals(targetCurrency)) {
				result.put(currency, BigDecimal.ONE);
				continue;
			}

			// 从数据库查询
			ExchangeRate rate = baseMapper.getRateByDate(currency, targetCurrency, rateDate);
			if (rate != null) {
				result.put(currency, rate.getRate());
				log.debug("从数据库获取汇率: {} -> {} = {} 日期:{}",
						currency, targetCurrency, rate.getRate(), rateDate);
			} else {
				missingCurrencies.add(currency);
			}
		}

		// 2. 如果有缺失的汇率,触发一次同步(会同步所有BASE_CURRENCIES)
		if (!missingCurrencies.isEmpty()) {
			log.info("数据库中缺失汇率: {} -> {} 日期:{}, 开始同步",
					missingCurrencies, targetCurrency, rateDate);

			try {
				syncRatesForDate(rateDate);
			} catch (Exception e) {
				log.error("同步汇率失败", e);
			}

			// 3. 再次查询缺失的汇率
			for (String currency : missingCurrencies) {
				ExchangeRate rate = baseMapper.getRateByDate(currency, targetCurrency, rateDate);
				if (rate != null) {
					result.put(currency, rate.getRate());
					log.debug("同步后获取汇率: {} -> {} = {} 日期:{}",
							currency, targetCurrency, rate.getRate(), rateDate);
				} else {
					log.error("同步后仍未找到汇率: {} -> {} 日期:{}", currency, targetCurrency, rateDate);
				}
			}
		}

		return result;
	}

	/**
	 * 同步指定日期的汇率数据
	 * 一次性从Fixer.io获取 EUR -> RUB, BYN, CNY 的汇率,然后计算并保存所有货币对CNY的汇率
	 * 
	 * @param rateDate 汇率日期
	 * @return 同步成功的记录数
	 */
	@Transactional(rollbackFor = Exception.class)
	public int syncRatesForDate(LocalDate rateDate) {
		int totalSaved = 0;

		// 1. 一次性调用API获取 EUR -> RUB, BYN, CNY 的汇率
		String symbols = String.join(",", SYNC_CURRENCIES);
		log.info("开始同步汇率: {} -> {} 日期: {}", FIXER_BASE_CURRENCY, symbols, rateDate);

		FixerApiResponse response = callFixerApiWithRetry(rateDate);

		if (response == null) {
			log.error("从Fixer.io获取汇率失败");
			return 0;
		}

		Map<String, BigDecimal> rates = response.getRates();
		if (rates == null || rates.isEmpty()) {
			log.warn("Fixer.io返回的汇率数据为空");
			return 0;
		}

		// 2. 获取 EUR->CNY 汇率
		BigDecimal eurToCny = rates.get(TARGET_CURRENCY);
		if (eurToCny == null) {
			log.error("未获取到 EUR->{} 汇率", TARGET_CURRENCY);
			return 0;
		}

		// 3. 保存 EUR->CNY (如果需要)
		if (notExistsByDateAndCurrencyPair(FIXER_BASE_CURRENCY, TARGET_CURRENCY, rateDate)) {
			saveExchangeRate(FIXER_BASE_CURRENCY, TARGET_CURRENCY, eurToCny, rateDate,
					response.getTimestamp(), SOURCE_FIXER);
			totalSaved++;
			log.info("保存汇率: {} -> {} = {} 日期: {}", FIXER_BASE_CURRENCY, TARGET_CURRENCY, eurToCny, rateDate);
		}

		// 4. 循环处理所有 BASE_CURRENCIES,计算并保存到CNY的汇率
		for (String baseCurrency : BASE_CURRENCIES) {
			BigDecimal eurToBase = rates.get(baseCurrency);

			if (eurToBase != null && eurToBase.compareTo(BigDecimal.ZERO) > 0) {
				// base->CNY = (EUR->CNY) / (EUR->base)
				BigDecimal baseToCny = eurToCny.divide(eurToBase, 6, RoundingMode.HALF_UP);

				if (notExistsByDateAndCurrencyPair(baseCurrency, TARGET_CURRENCY, rateDate)) {
					saveExchangeRate(baseCurrency, TARGET_CURRENCY, baseToCny, rateDate,
							response.getTimestamp(), SOURCE_FIXER);
					totalSaved++;
					log.info("计算并保存汇率: {} -> {} = {} (通过EUR计算) 日期: {}",
							baseCurrency, TARGET_CURRENCY, baseToCny, rateDate);
				}
			}
			else {
				log.warn("未获取到 EUR->{} 汇率,无法计算 {}->{}",
						baseCurrency, baseCurrency, TARGET_CURRENCY);
			}
		}

		return totalSaved;
	}

	/**
	 * 批量检查指定日期的多个货币对是否不存在
	 * 
	 * @param baseCurrencies 基准货币列表
	 * @param targetCurrency 目标货币
	 * @param rateDate       汇率日期
	 * @return 不存在的货币代码列表
	 */
	private List<String> findMissingRates(List<String> baseCurrencies, String targetCurrency, LocalDate rateDate) {
		List<String> missing = new ArrayList<>();
		for (String baseCurrency : baseCurrencies) {
			if (notExistsByDateAndCurrencyPair(baseCurrency, targetCurrency, rateDate)) {
				missing.add(baseCurrency);
			}
		}
		return missing;
	}

	/**
	 * 从Fixer.io同步昨日汇率数据(用于定时任务)
	 * 获取昨日的EOD汇率,作为今日订单使用的汇率
	 * 
	 * 优化策略:
	 * 1. Fixer.io免费计划base只能是EUR
	 * 2. 检查是否有任一货币对CNY的汇率缺失
	 * 3. 如有缺失,一次性获取所有汇率并计算保存
	 *
	 * @return 同步成功的记录数
	 */
	@Transactional(rollbackFor = Exception.class)
	public int syncLatestRatesFromFixer() {
		List<String> apiKeys = fixerApiProperties.getKeys();
		if (apiKeys.isEmpty()) {
			log.error("Fixer.io API Keys未配置,请在application.yml中配置fixer.api.keys");
			return 0;
		}

		LocalDate yesterday = LocalDate.now().minusDays(1);

		// 检查是否有缺失的汇率: 只要任一 BASE_CURRENCIES->CNY 不存在就需要同步
		List<String> missingRates = findMissingRates(BASE_CURRENCIES, TARGET_CURRENCY, yesterday);

		if (missingRates.isEmpty()) {
			log.info("所有汇率已存在,跳过同步: 日期: {}", yesterday);
			return 0;
		}

		log.info("检测到缺失的汇率: {} -> {}, 开始同步", missingRates, TARGET_CURRENCY);

		try {
			return syncRatesForDate(yesterday);
		}
		catch (Exception e) {
			log.error("同步汇率失败", e);
			return 0;
		}
	}

	/**
	 * 检查指定日期和货币对的汇率是否已存在
	 *
	 * @param baseCurrencyCode   基准币种
	 * @param targetCurrencyCode 目标币种
	 * @param rateDate           汇率日期
	 * @return 存在返回true,否则返回false
	 */
	public boolean notExistsByDateAndCurrencyPair(String baseCurrencyCode, String targetCurrencyCode,
												  LocalDate rateDate) {
		return baseMapper.existsByDateAndCurrencyPair(baseCurrencyCode, targetCurrencyCode, rateDate) <= 0;
	}

	/**
	 * 保存汇率记录
	 *
	 * @param baseCurrencyCode   基准币种
	 * @param targetCurrencyCode 目标币种
	 * @param rate               汇率
	 * @param rateDate           汇率日期
	 * @param apiTimestamp       API时间戳
	 * @param source             数据来源
	 */
	@Transactional(rollbackFor = Exception.class)
	public void saveExchangeRate(String baseCurrencyCode, String targetCurrencyCode, BigDecimal rate,
								 LocalDate rateDate, Long apiTimestamp, String source) {
		ExchangeRate exchangeRate = new ExchangeRate();
		exchangeRate.setBaseCurrencyCode(baseCurrencyCode);
		exchangeRate.setTargetCurrencyCode(targetCurrencyCode);
		exchangeRate.setRate(rate);
		exchangeRate.setRateDate(rateDate);
		exchangeRate.setApiTimestamp(apiTimestamp);
		exchangeRate.setSource(source);
		LocalDateTime now = LocalDateTime.now();
		exchangeRate.setCreatedAt(now);
		exchangeRate.setUpdatedAt(now);
		save(exchangeRate);
	}

	public ExchangeRate getLatestRate(String baseCurrency, String targetCurrency) {
		return this.baseMapper.getLatestRate(baseCurrency, targetCurrency);
	}

	public ExchangeRate getRateByDate(String baseCurrency, String targetCurrency, LocalDate date) {
		return this.baseMapper.getRateByDate(baseCurrency, targetCurrency, date);
	}
}