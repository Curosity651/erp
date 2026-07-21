package com.erp.admin.system.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.erp.admin.system.model.entity.ExchangeRate;
import com.erp.admin.system.service.ExchangeRateService;
import com.erp.admin.tenant.service.TenantIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.common.model.result.SystemResultCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 汇率管理控制器
 *
 * @author system
 */
@Tag(name = "汇率管理")
@RestController
@RequestMapping("/system/exchange-rate")
@RequiredArgsConstructor
public class ExchangeRateController {

	private final ExchangeRateService exchangeRateService;

	private final TenantIdentityService tenantIdentityService;

	@Operation(summary = "获取最新汇率")
	@GetMapping("/latest")
	public ApiResult<BigDecimal> getLatestRate(
			@RequestParam(defaultValue = "USD") String baseCurrency,
			@RequestParam(defaultValue = "CNY") String targetCurrency) {
		ExchangeRate rate = exchangeRateService.getLatestRate(baseCurrency, targetCurrency);
		if (rate != null) {
			return ApiResult.ok(rate.getRate());
		}
		// 如果没有找到，尝试同步后再获取
		exchangeRateService.syncLatestRatesFromFixer();
		rate = exchangeRateService.getLatestRate(baseCurrency, targetCurrency);
		if (rate != null) {
			return ApiResult.ok(rate.getRate());
		}
		return ApiResult.failed(SystemResultCode.BAD_REQUEST, "未找到汇率数据");
	}

	@Operation(summary = "获取指定日期汇率")
	@GetMapping("/by-date")
	public ApiResult<BigDecimal> getRateByDate(
			@RequestParam(defaultValue = "USD") String baseCurrency,
			@RequestParam(defaultValue = "CNY") String targetCurrency,
			@RequestParam String rateDate) {
		LocalDate date = LocalDate.parse(rateDate);
		ExchangeRate rate = exchangeRateService.getRateByDate(baseCurrency, targetCurrency, date);
		if (rate != null) {
			return ApiResult.ok(rate.getRate());
		}
		// 如果没有找到，尝试同步后再获取
		exchangeRateService.syncRatesForDate(date);
		rate = exchangeRateService.getRateByDate(baseCurrency, targetCurrency, date);
		if (rate != null) {
			return ApiResult.ok(rate.getRate());
		}
		return ApiResult.failed(SystemResultCode.BAD_REQUEST, "未找到汇率数据");
	}

	@Operation(summary = "手动同步汇率(同步昨日汇率)")
	@PostMapping("/sync")
	public Map<String, Object> syncRates() {
		// 仅海外仓平台可手动触发外部 Fixer 同步,防止任意登录用户消耗外部配额(DoS)
		if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM
				.equals(tenantIdentityService.currentIdentity(null).getIdentityType())) {
			throw new BusinessException(403, "仅海外仓平台可手动同步汇率");
		}
		Map<String, Object> result = new HashMap<>();
		try {
			int count = exchangeRateService.syncLatestRatesFromFixer();
			result.put("success", true);
			result.put("count", count);
			result.put("message", "同步成功,共同步" + count + "条记录");
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", "同步失败: " + e.getMessage());
		}
		return result;
	}

}
