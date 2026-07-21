import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'

/**
 * 获取最新汇率
 * @param baseCurrency 基准币种，默认 USD
 * @param targetCurrency 目标币种，默认 CNY
 */
export function getLatestExchangeRate(baseCurrency = 'USD', targetCurrency = 'CNY') {
  return httpClient.get<ApiResult<number>>('/system/exchange-rate/latest', {
    params: { baseCurrency, targetCurrency }
  })
}

/**
 * 获取指定日期汇率
 * @param baseCurrency 基准币种
 * @param targetCurrency 目标币种
 * @param rateDate 汇率日期 (YYYY-MM-DD)
 */
export function getExchangeRateByDate(
  baseCurrency: string,
  targetCurrency: string,
  rateDate: string
) {
  return httpClient.get<ApiResult<number>>('/system/exchange-rate/by-date', {
    params: { baseCurrency, targetCurrency, rateDate }
  })
}
